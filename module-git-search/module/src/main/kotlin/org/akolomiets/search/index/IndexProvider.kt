package org.akolomiets.search.index

import org.akolomiets.search.config.ApplicationProperties
import org.apache.commons.io.FileUtils
import org.springframework.stereotype.Service
import java.io.File
import javax.annotation.PreDestroy

/**
 * Class that stores and provides access to both main index and reserve index copy.
 *
 * **Important** both main index and reserve copy index should **NOT** be used simultaneously to avoid
 * data synchronization problems.
 * Use [getAccessibleIndex] to get current accessible index instead.
 *
 * @author akolomiets
 * @since 1.0.0
 */
@Service("search.indexProvider")
class IndexProvider(
    applicationProperties: org.akolomiets.search.config.ApplicationProperties
) {
    private val dataDir = applicationProperties.dataDir

    private val mainIndex = Index("$dataDir/$INDEX_DIR/$MAIN_INDEX_DIR")
    private val reserveIndex = Index("$dataDir/$INDEX_DIR/$RESERVE_INDEX_DIR")

    private var accessibleIndex = mainIndex

    /**
     * Switches to usage of main index.
     */
    fun useMainIndex() {
        accessibleIndex = mainIndex
    }

    /**
     * Switches to usage of reserve copy index.
     */
    fun useReserveIndex() {
        accessibleIndex = reserveIndex
    }

    /**
     * **Preferred**
     *
     * Returns the index that is currently in use.
     */
    fun getAccessibleIndex(): Index {
        return accessibleIndex
    }

    /**
     * **Use carefully**
     *
     * Returns the main index.
     */
    fun getMainIndex(): Index {
        return mainIndex
    }

    /**
     * **Use carefully**
     *
     * Returns the reserve copy index.
     */
    fun getReserveIndex(): Index {
        return reserveIndex
    }

    /**
     * Updates reserve copy index by replacing an existing folder with a new copy of main index.
     */
    fun overrideReserveCopy() {
        val srcFile = File("$dataDir/$INDEX_DIR/$MAIN_INDEX_DIR")
        val destFile = File("$dataDir/$INDEX_DIR/$RESERVE_INDEX_DIR")
        FileUtils.copyDirectory(srcFile, destFile)
    }

    /**
     * Closes both main and reserve copy index.
     */
    @PreDestroy
    fun closeIndex() {
        mainIndex.close()
        reserveIndex.close()
    }

    companion object {

        /**
         * Parent directory name for storing both indexes.
         */
        const val INDEX_DIR = "index"

        /**
         * Directory for storing main index.
         */
        const val MAIN_INDEX_DIR = "main"

        /**
         * Directory for storing reserve copy index.
         */
        const val RESERVE_INDEX_DIR = "reserve"
    }
}