package org.akolomiets.search.index.cache

import org.mapdb.DBMaker
import org.springframework.stereotype.Service

/**
 * Class for providing new instances of [PersistableCache].
 *
 * @author akolomiets
 * @since 1.0.0
 */
@Service("search.cacheProvider")
class CacheProvider {
    private val persistedCache = DBMaker
        .tempFileDB()
        .closeOnJvmShutdown()
        .make()

    /**
     * Creates new cache for provided repository URI.
     *
     * @param repositoryUri the repository SSH URI
     *
     * @return created cache
     */
    fun getCache(repositoryUri: String): PersistableCache {
        // TODO store existing and provide if present
        return PersistableCache(persistedCache, repositoryUri)
    }
}