package org.khorum.oss.ino.core.config

import liquibase.integration.spring.SpringLiquibase
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

/**
 * Explicit Liquibase wiring.
 *
 * Spring Boot 4.1.0-M1's autoconfig for Liquibase doesn't activate in our
 * setup (no `LiquibaseAutoConfiguration` evaluated during boot — likely a
 * milestone artifact issue or a Liquibase 5 / Spring Boot 4 condition gap).
 * Wiring `SpringLiquibase` as an explicit bean side-steps the autoconfig
 * entirely and makes the dependency obvious in the codebase.
 *
 * If Spring Boot 4.x autoconfig starts picking this up cleanly later, this
 * bean can be removed and `spring.liquibase.change-log` in application.yml
 * will take over.
 */
@Configuration
class LiquibaseConfig {

    @Bean
    fun springLiquibase(dataSource: DataSource): SpringLiquibase = SpringLiquibase().apply {
        this.dataSource = dataSource
        this.changeLog = "classpath:/db/changelog/db.changelog-master.yaml"
    }
}
