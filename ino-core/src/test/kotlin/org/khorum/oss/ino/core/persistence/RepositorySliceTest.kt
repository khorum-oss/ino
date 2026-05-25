package org.khorum.oss.ino.core.persistence

import com.zaxxer.hikari.HikariDataSource
import liquibase.integration.spring.SpringLiquibase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import org.khorum.oss.ino.core.config.JacksonConfig
import org.khorum.oss.ino.test.FixedClockConfig
import org.khorum.oss.ino.test.InoHomeExtension
import org.springframework.jdbc.core.simple.JdbcClient
import tools.jackson.databind.json.JsonMapper
import java.time.Clock

/**
 * Shared setup for repository slice tests: per-test tempdir, fresh SQLite DB
 * with Liquibase applied, deterministic clock, configured ObjectMapper.
 *
 * Each `@Test` method gets a brand-new database file under
 * `${ino.home}/state.db`, so tests are fully isolated from one another and
 * can run in parallel processes without collision.
 */
@ExtendWith(InoHomeExtension::class)
abstract class RepositorySliceTest {

    @JvmField
    @RegisterExtension
    val inoHome = InoHomeExtension()

    protected lateinit var jdbc: JdbcClient
    protected lateinit var dataSource: HikariDataSource

    protected val clock: Clock = FixedClockConfig().fixedClock()
    protected val mapper: JsonMapper = JacksonConfig().objectMapper()

    @BeforeEach
    fun setUpDatabase() {
        val dbFile = inoHome.homeDir.resolve("state.db")
        dataSource = HikariDataSource().apply {
            jdbcUrl = "jdbc:sqlite:$dbFile"
            driverClassName = "org.sqlite.JDBC"
            maximumPoolSize = 1
            connectionInitSql = "PRAGMA journal_mode=WAL; PRAGMA foreign_keys=ON; PRAGMA synchronous=NORMAL;"
        }
        SpringLiquibase().apply {
            dataSource = this@RepositorySliceTest.dataSource
            changeLog = "classpath:/db/changelog/db.changelog-master.yaml"
            resourceLoader = org.springframework.core.io.DefaultResourceLoader()
        }.afterPropertiesSet()

        jdbc = JdbcClient.create(dataSource)
    }

    @AfterEach
    fun tearDownDatabase() {
        dataSource.close()
    }
}
