package org.khorum.oss.ino.core.agent

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.khorum.oss.ino.dsl.Agent
import org.khorum.oss.ino.dsl.agent
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AgentRegistryTest {

    private fun localAgent(named: String): Agent = agent {
        name = named
        provider {
            local {
                model = "test-model"
                host = "http://localhost:11434"
            }
        }
    }

    @Test
    fun `findByName returns the registered agent`() {
        val registry = AgentRegistry(listOf(localAgent("foo"), localAgent("bar")))

        assertEquals("foo", registry.findByName("foo")?.name)
        assertEquals("bar", registry.findByName("bar")?.name)
    }

    @Test
    fun `findByName returns null for an unknown agent`() {
        val registry = AgentRegistry(listOf(localAgent("foo")))
        assertNull(registry.findByName("does-not-exist"))
    }

    @Test
    fun `requireByName throws NoSuchElementException for an unknown agent`() {
        val registry = AgentRegistry(emptyList())
        val ex = assertThrows<NoSuchElementException> { registry.requireByName("missing") }
        kotlin.test.assertTrue(ex.message!!.contains("missing"))
    }

    @Test
    fun `names returns a sorted list`() {
        val registry = AgentRegistry(listOf(localAgent("zebra"), localAgent("alpha"), localAgent("mike")))
        assertEquals(listOf("alpha", "mike", "zebra"), registry.names())
    }

    @Test
    fun `duplicate names fail fast at construction`() {
        val ex = assertThrows<IllegalArgumentException> {
            AgentRegistry(listOf(localAgent("dup"), localAgent("dup")))
        }
        kotlin.test.assertTrue(ex.message!!.contains("Duplicate") && ex.message!!.contains("dup"))
    }
}
