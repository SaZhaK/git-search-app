package org.akolomiets.search.index.cache

import org.akolomiets.search.index.data.FileDetails
import org.akolomiets.search.index.data.LineSummary
import org.mapdb.DB
import org.mapdb.Serializer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.function.Consumer

/**
 * Class that stores information about processed lines and files from one particular repository.
 * The data can be written down to a temporal file using MapDB to reduce heap size.
 *
 * @author akolomiets
 * @since 1.0.0
 */
class PersistableCache(
    private val persistedCache: DB,
    repositoryUri: String
) {
    private var linesCache = persistedCache
        .hashMap("$repositoryUri-lines", Serializer.JAVA, Serializer.STRING)
        .createOrOpen()

    private var filesCache = persistedCache
        .hashSet("$repositoryUri-files", Serializer.JAVA)
        .createOrOpen()

    /**
     * Writes the provided information about lines and files to cache and persists it in memory.
     * The information lists themselves will be cleared after this operation.
     *
     * @param linesInformation information about processed lines
     * @param filesInformation information about processed files
     */
    fun writeAndPersist(
        linesInformation: ConcurrentHashMap<LineSummary, String>,
        filesInformation: ConcurrentLinkedQueue<FileDetails>
    ) {
        linesInformation.forEach {
            linesCache.merge(
                it.key,
                it.value
            ) { persistedBranchList, newBranchList -> "$persistedBranchList $newBranchList" }
        }

        filesInformation.forEach {
            filesCache.add(it)
        }

        persistedCache.commit()

        // TODO looking strange here?
        linesInformation.clear()
        filesInformation.clear()
    }

    /**
     * Reads data from caches and applies provided consumers to each element.
     *
     * @param lineAction consumer action to be applied to each piece of line information
     * @param fileAction consumer action to be applied to each piece of file information
     */
    fun readAndClear(lineAction: Consumer<Map.Entry<Any, String>>, fileAction: Consumer<Any>) {
        linesCache.forEach { lineAction.accept(it) }
        linesCache.clear()

        filesCache.forEach { fileAction.accept(it) }
        filesCache.clear()
    }
}