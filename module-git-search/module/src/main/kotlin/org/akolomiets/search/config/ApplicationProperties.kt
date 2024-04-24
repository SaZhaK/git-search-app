package org.akolomiets.search.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

/**
 * Configurable application properties.
 *
 * @author akolomiets
 * @since 1.0.0
 */
@Configuration("search.applicationProperties")
open class ApplicationProperties {

    /**
     * The directory containing configuration files
     */
    @Value("\${configDir.parameter}")
    lateinit var configDir: String

    /**
     * The directory containing data files
     */
    @Value("\${dataDir.parameter}")
    lateinit var dataDir: String

    /**
     * The name of file containing repositories list
     */
    @Value("\${repositories.parameter}")
    lateinit var repositories: String
}