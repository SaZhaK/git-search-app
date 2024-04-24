package org.akolomiets.search;

import org.apache.lucene.search.BooleanQuery;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main class for starting the application.
 *
 * @author akolomiets
 * @since 1.0.0
 */
@EnableScheduling
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class);

        // TODO temp workaround
        System.setProperty("org.apache.lucene.maxClauseCount", Integer.toString(Integer.MAX_VALUE));
        BooleanQuery.setMaxClauseCount(Integer.MAX_VALUE);
    }
}
