package org.khorum.oss.ino.core.agent

import org.khorum.oss.ino.dsl.AgentDefinition

/**
 * In-memory registry of konstellation DSL [AgentDefinition] declarations keyed by name.
 *
 * The registry is constructed once at startup (see `SampleAgentsConfig` for the
 * MVP bean wiring). Future phases may load agents from filesystem or external
 * registries; the lookup contract stays the same.
 */
class AgentRegistry(agentDefinitions: List<AgentDefinition>) {

    private val byName: Map<String, AgentDefinition> = agentDefinitions.associateBy(AgentDefinition::name).also {
        val duplicates = agentDefinitions.groupingBy(AgentDefinition::name)
            .eachCount()
            .filterValues { c -> c > 1 }
            .keys

        require(it.size == agentDefinitions.size) {
            "Duplicate agent names registered: $duplicates"
        }
    }

    fun findByName(name: String): AgentDefinition? = byName[name]

    fun requireByName(name: String): AgentDefinition =
        byName[name] ?: throw NoSuchElementException("No agent registered with name '$name'")

    fun names(): List<String> = byName.keys.sorted()

    fun all(): List<AgentDefinition> = byName.values.toList()
}
