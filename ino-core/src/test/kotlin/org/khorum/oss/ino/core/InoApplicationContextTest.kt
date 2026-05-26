package org.khorum.oss.ino.core

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.khorum.oss.ino.core.persistence.ConversationStore
import org.khorum.oss.ino.core.persistence.MessageRepository
import org.khorum.oss.ino.core.persistence.SessionRepository
import org.khorum.oss.ino.core.persistence.ToolInvocationRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.deleteRecursively
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Smoke test that the full Spring context boots: Liquibase migrations apply
 * against a tempdir SQLite DB, every repository bean wires up, and the
 * `ConversationStore` facade is reachable. Catches application.yml / autoconfig
 * regressions that pure slice tests would miss.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class InoApplicationContextTest {

    @Autowired private lateinit var sessions: SessionRepository
    @Autowired private lateinit var messages: MessageRepository
    @Autowired private lateinit var invocations: ToolInvocationRepository
    @Autowired private lateinit var conversationStore: ConversationStore
    @Autowired private lateinit var jdbc: JdbcClient

    @Test
    fun `application context loads with all persistence beans`() {
        assertNotNull(sessions)
        assertNotNull(messages)
        assertNotNull(invocations)
        assertNotNull(conversationStore)

        val tables = jdbc.sql("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name")
            .query(String::class.java)
            .list()
        assertContains(tables, "sessions")
        assertContains(tables, "messages")
        assertContains(tables, "tool_invocations")
    }

    @Test
    fun `conversationStore can persist a session through the booted context`() {
        val session = conversationStore.startSession("writer", "anthropic", "claude-opus-4-7")
        conversationStore.appendUser(session.id, "hello")

        val loaded = conversationStore.messagesFor(session.id)
        assertEquals(1, loaded.size)
        assertEquals("hello", loaded.single().content)
    }

    companion object {
        private lateinit var tempHome: Path

        @JvmStatic
        @BeforeAll
        fun allocateInoHome() {
            tempHome = Files.createTempDirectory("ino-bootsmoke-")
            System.setProperty("ino.home", tempHome.toString())
        }

        @JvmStatic
        @AfterAll
        @OptIn(kotlin.io.path.ExperimentalPathApi::class)
        fun cleanupInoHome() {
            System.clearProperty("ino.home")
            tempHome.deleteRecursively()
        }

        @JvmStatic
        @DynamicPropertySource
        fun overrideDatasource(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { "jdbc:sqlite:${System.getProperty("ino.home")}/state.db" }
        }
    }
}
