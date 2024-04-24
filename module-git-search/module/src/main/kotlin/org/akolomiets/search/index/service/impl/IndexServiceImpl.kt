package org.akolomiets.search.index.service.impl

import org.akolomiets.search.config.ApplicationProperties
import org.akolomiets.search.dto.IndexInfoDto
import org.akolomiets.search.dto.RepositoryInfoDto
import org.akolomiets.search.git.GitService
import org.akolomiets.search.index.DocumentProvider
import org.akolomiets.search.index.Index
import org.akolomiets.search.index.Index.Companion.BRANCH
import org.akolomiets.search.index.Index.Companion.BRANCHES
import org.akolomiets.search.index.Index.Companion.CONTENT
import org.akolomiets.search.index.Index.Companion.PATH
import org.akolomiets.search.index.IndexProvider
import org.akolomiets.search.index.analyzer.CodeAnalyzer
import org.akolomiets.search.index.cache.CacheProvider
import org.akolomiets.search.index.cache.PersistableCache
import org.akolomiets.search.index.data.FileDetails
import org.akolomiets.search.index.data.LineSummary
import org.akolomiets.search.index.service.IndexService
import org.akolomiets.search.index.service.impl.SearchServiceImpl.Companion.UNKNOWN_INFO
import org.apache.commons.io.FileUtils
import org.apache.lucene.analysis.core.WhitespaceAnalyzer
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.index.Term
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.PrefixQuery
import org.apache.lucene.search.TermQuery
import org.eclipse.jgit.api.Git
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.File
import java.io.UncheckedIOException
import java.nio.file.Files
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.function.Consumer
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.io.path.pathString

/**
 * @author akolomiets
 * @since 1.0.0
 */
@Component("search.indexService")
class IndexServiceImpl constructor(
    applicationProperties: org.akolomiets.search.config.ApplicationProperties,
    private val analyzer: CodeAnalyzer,
    private val gitService: GitService,
    private val cacheProvider: CacheProvider,
    private val indexProvider: IndexProvider,
    private val documentProvider: DocumentProvider
) : IndexService {
    private val repositoriesDir = "${applicationProperties.dataDir}/$REPOSITORIES_DIR"

    private val updateDataCache = ConcurrentHashMap<String, UpdateData>()

    private val availableProcessors = Runtime.getRuntime().availableProcessors()
    private val repositoryThreads = Executors.newFixedThreadPool(availableProcessors)
    private val branchThreads = Executors.newFixedThreadPool(availableProcessors)
    private val cacheThreads = Executors.newFixedThreadPool(1)

    private val updateInProgress = AtomicBoolean(false)

    private lateinit var lastUpdateTime: LocalDateTime

    private class UpdateData(
        val cache: PersistableCache,
        val branchesToHeads: Map<String, String>
    )

    override fun updateIndex() {
        if (updateInProgress.compareAndSet(false, true)) {
            try {
                LOGGER.info("Checking repositories for update")
                val repositories = gitService.getRepositories()
                val repositoriesToBeUpdated = repositories
                    .parallelStream()
                    .filter { gitService.repositoryHasUpdates(it.remote) }
                    .toList()
                if (repositoriesToBeUpdated.isNotEmpty()) {
                    LOGGER.info("Started index update")

                    indexProvider.useReserveIndex()

                    updateRepositories(repositoriesToBeUpdated, indexProvider.getMainIndex())
                    LOGGER.info("Updated main index")

                    indexProvider.useMainIndex()

                    indexProvider.overrideReserveCopy()
                    LOGGER.info("Updated reserve index")
                }
                lastUpdateTime = LocalDateTime.now()
            } finally {
                updateInProgress.set(false)
            }
        }
    }

    override fun getIndexInfo(): org.akolomiets.search.dto.IndexInfoDto {
        val dateTimeFormatter = DateTimeFormatter.ofPattern("hh:mm")
        val formattedLastUpdateTime = dateTimeFormatter.format(lastUpdateTime)
        return org.akolomiets.search.dto.IndexInfoDto(formattedLastUpdateTime)
    }

    private fun updateRepositories(repositories: List<org.akolomiets.search.dto.RepositoryInfoDto>, index: Index) {
        val analyzersMap = mapOf(
            CONTENT to analyzer,
            BRANCHES to WhitespaceAnalyzer()
        )
        val perFieldAnalyzerWrapper = PerFieldAnalyzerWrapper(StandardAnalyzer(), analyzersMap)

        IndexWriter(index.lineIndex, IndexWriterConfig(perFieldAnalyzerWrapper)).use { linesIndexWriter ->
            IndexWriter(index.fileIndex, IndexWriterConfig(StandardAnalyzer())).use { filesIndexWriter ->
                val countDownLatch = CountDownLatch(repositories.size)
                for (repository in repositories) {
                    repositoryThreads.execute {
                        try {
                            updateRepository(repository, linesIndexWriter, filesIndexWriter)
                        } finally {
                            countDownLatch.countDown()
                        }
                    }
                }
                countDownLatch.await()

                writeCachesToIndex(linesIndexWriter, filesIndexWriter)
            }
        }
    }

    private fun updateRepository(
        repository: org.akolomiets.search.dto.RepositoryInfoDto,
        linesIndexWriter: IndexWriter,
        filesIndexWriter: IndexWriter
    ) {
        val repositoryUri = repository.remote
        val remoteRepository = repositoryUri.removePrefix("https://").removeSuffix(".git")
        val repositoryName = remoteRepository.substring(remoteRepository.lastIndexOf("/") + 1)

        try {
            val branchesToHeads = gitService.fetchBranches(repositoryUri)
            val branches = gitService.getBranchesForUpdate(repositoryUri, branchesToHeads)

            val totalLinesInfo = ConcurrentHashMap<LineSummary, String>()
            val totalFilesInfo = ConcurrentLinkedQueue<FileDetails>()

            val cache = cacheProvider.getCache(repositoryUri)
            updateDataCache[repositoryUri] = UpdateData(cache, branchesToHeads)

            // TODO add better balancing
            if (branches.size <= BATCH_SIZE) {
                // Processing in the same thread
                processBatch(
                    1,
                    branches,
                    repository,
                    repositoryName,
                    totalLinesInfo,
                    totalFilesInfo,
                    Object(),
                    AtomicBoolean(false),
                    ReentrantReadWriteLock(),
                    cache
                )
            } else {
                // Processing batches in separate threads
                val mutex = Object()
                val isPersistingCache = AtomicBoolean(false)
                val lock = ReentrantReadWriteLock()

                val batchSize = branches.size / availableProcessors
                val batches = branches.chunked(batchSize)
                val countDownLatch = CountDownLatch(batches.size)
                batches.forEachIndexed { number, batch ->
                    branchThreads.execute {
                        try {
                            processBatch(
                                number + 1,
                                batch,
                                repository,
                                repositoryName,
                                totalLinesInfo,
                                totalFilesInfo,
                                mutex,
                                isPersistingCache,
                                lock,
                                cache
                            )
                        } finally {
                            countDownLatch.countDown()
                        }
                    }
                }
                countDownLatch.await()
            }

            // TODO check
            deleteOutdatedDocuments(repositoryName, branches, linesIndexWriter, filesIndexWriter)

            LOGGER.info("Persisting cache...")
            cache.writeAndPersist(totalLinesInfo, totalFilesInfo)
            LOGGER.info("Persisted cache")

            LOGGER.info("Indexed $repositoryName")
        } finally {
            FileUtils.deleteDirectory(File("$repositoriesDir/$repositoryName"))
        }
    }

    private fun writeCachesToIndex(
        linesIndexWriter: IndexWriter,
        filesIndexWriter: IndexWriter
    ) {
        for ((repositoryUri, updateData) in updateDataCache) {
            val cache = updateData.cache
            val branchesToHeads = updateData.branchesToHeads
            val lineAction = Consumer<Map.Entry<Any, String>> {
                linesIndexWriter.addDocument(documentProvider.lineInfoToDocument(it.key as LineSummary, it.value))
            }
            val fileAction = Consumer<Any> {
                filesIndexWriter.addDocument(documentProvider.fileInfoToDocument(it as FileDetails))
            }

            // TODO This should be a transaction
            cache.readAndClear(lineAction, fileAction)
            gitService.setRepositoryHeads(branchesToHeads, repositoryUri)
        }
    }

    private fun deleteOutdatedDocuments(
        repositoryName: String,
        branches: List<String>,
        linesIndexWriter: IndexWriter,
        filesIndexWriter: IndexWriter
    ) {
        val pathQuery = PrefixQuery(Term(PATH, repositoryName))

        val lineQueryBuilder = BooleanQuery.Builder()
        lineQueryBuilder.add(pathQuery, BooleanClause.Occur.MUST)
        for (branch in branches) {
            val branchQuery = TermQuery(Term(BRANCHES, branch))
            lineQueryBuilder.add(branchQuery, BooleanClause.Occur.SHOULD)
        }
        val lineQuery = lineQueryBuilder.build()
        linesIndexWriter.deleteDocuments(lineQuery)

        val fileQueryBuilder = BooleanQuery.Builder()
        fileQueryBuilder.add(pathQuery, BooleanClause.Occur.MUST)
        for (branch in branches) {
            val branchQuery = TermQuery(Term(BRANCH, branch))
            fileQueryBuilder.add(branchQuery, BooleanClause.Occur.SHOULD)
        }
        val fileQuery = fileQueryBuilder.build()
        filesIndexWriter.deleteDocuments(fileQuery)
    }

    private fun processBatch(
        batchNumber: Int,
        branches: List<String>,
        repository: org.akolomiets.search.dto.RepositoryInfoDto,
        repositoryName: String,
        linesInformation: ConcurrentHashMap<LineSummary, String>,
        filesInformation: ConcurrentLinkedQueue<FileDetails>,
        mutex: Object,
        isPersistingCache: AtomicBoolean,
        lock: ReentrantReadWriteLock,
        cache: PersistableCache
    ) {
        val repositoryUri = repository.remote
        val repositoryType = repository.type

        val batch = "batch-$batchNumber"
        val localBatchRepository = "$repositoriesDir/$repositoryName/$batch"

        val git = gitService.cloneRepository(repositoryUri, localBatchRepository)

        for (branch in branches) {
            LOGGER.info("$batch: Indexing $branch in $repositoryName")
            try {
                gitService.checkoutBranch(git, branch)

                val fullPath = Paths.get(localBatchRepository)
                Files.walk(fullPath).use { stream ->
                    stream
                        .filter(Files::isRegularFile)
                        .filter { !it.pathString.substring(0, it.pathString.lastIndexOf("/")).contains(".") }
                        .forEach {
                            val filePath = it.pathString
                            val remotePath = filePath.removePrefix("$repositoriesDir/").replace("$batch/", "")

                            lock.read {
                                readLinesInfo(linesInformation, remotePath, filePath, branch)
                                readFileInfo(
                                    filesInformation,
                                    git,
                                    filePath,
                                    repositoryType,
                                    branch,
                                    repositoryName,
                                    remotePath
                                )
                            }

                            if (linesInformation.size > LINES_CACHE_SIZE || filesInformation.size > FILES_CACHE_SIZE) {
                                if (isPersistingCache.compareAndSet(false, true)) {
                                    lock.write {
                                        cacheThreads.execute {
                                            LOGGER.info("Persisting cache...")
                                            cache.writeAndPersist(linesInformation, filesInformation)
                                            LOGGER.info("Persisted cache")

                                            isPersistingCache.set(false)
                                            synchronized(mutex) {
                                                mutex.notifyAll()
                                            }
                                        }
                                    }
                                } else {
                                    synchronized(mutex) {
                                        mutex.wait()
                                    }
                                }
                            }
                        }
                }
                LOGGER.info("$batch: Indexed $branch in $repositoryName")
            } catch (e: Exception) {
                LOGGER.error("Can't process $branch in $repositoryUri")
            }
        }
    }

    private fun readLinesInfo(
        linesInformation: ConcurrentHashMap<LineSummary, String>,
        remotePath: String,
        localPath: String,
        branch: String
    ) {
        val readLines = readFile(localPath)
        readLines.mapIndexed { index, lineContent ->
            val lineNumber = (index + 1)
            val lineSummary = LineSummary(remotePath, lineContent, lineNumber)
            linesInformation.merge(lineSummary, branch) { branchList, newBranch -> "$branchList $newBranch" }
        }
    }

    private fun readFileInfo(
        fileDetailsList: ConcurrentLinkedQueue<FileDetails>,
        git: Git,
        filePath: String,
        repositoryType: String,
        branch: String,
        repository: String,
        remotePath: String
    ) {
        val fileRelativePath = remotePath.removePrefix("$repository/")
        val lastCommit = gitService.getLastCommit(git, fileRelativePath)

        var authorEmail = UNKNOWN_INFO
        var date = UNKNOWN_INFO
        lastCommit?.let {
            val authorIdent = lastCommit.authorIdent

            val time = authorIdent.`when`
            val simpleDateFormat = SimpleDateFormat("dd.MM.yyyy")

            authorEmail = authorIdent.emailAddress
            date = simpleDateFormat.format(time)
        }

        val lines = readFile(filePath)
        val javaPackageLine = lines.firstOrNull { it.trim().startsWith("package") }
        val javaPackage =
            javaPackageLine?.removePrefix("package")?.trim()?.removeSuffix(";")?.lowercase() ?: "NO_PACKAGE"

        val type = repositoryType.lowercase()

        val fileDetails = FileDetails(
            authorEmail,
            date,
            javaPackage,
            remotePath,
            type,
            branch,
            repository
        )
        fileDetailsList.add(fileDetails)
    }

    private fun readFile(fullPath: String): List<String> {
        return try {
            val lines = Files.lines(Paths.get(fullPath))
            val data = lines.toList()
            lines.close()
            data
        } catch (e: UncheckedIOException) {
            // Ignoring git, ssl and other unreadable files
            emptyList()
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(IndexServiceImpl::class.java)

        private const val BATCH_SIZE = 20

        private const val LINES_CACHE_SIZE = 2_000_000

        private const val FILES_CACHE_SIZE = 200_000

        private const val REPOSITORIES_DIR = "repositories"
    }
}