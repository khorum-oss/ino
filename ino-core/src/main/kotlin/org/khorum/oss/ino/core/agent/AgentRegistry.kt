package org.khorum.oss.ino.core.agent

import org.khorum.oss.ino.dsl.Agent

/**
 * In-memory registry of konstellation DSL [Agent] declarations keyed by name.
 *
 * The registry is constructed once at startup (see `SampleAgentsConfig` for the
 * MVP bean wiring). Future phases may load agents from filesystem or external
 * registries; the lookup contract stays the same.
 */
class AgentRegistry(agents: List<Agent>) {

    private val byName: Map<String, Agent> = agents.associateBy(Agent::name).also {
        require(it.size == agents.size) {
            "Duplicate agent names registered: ${agents.groupingBy(Agent::name).eachCount().filterValues { c -> c > 1 }.keys}"
        }
    }

    fun findByName(name: String): Agent? = byName[name]

    fun requireByName(name: String): Agent =
        byName[name] ?: throw NoSuchElementException("No agent registered with name '$name'")

    fun names(): List<String> = byName.keys.sorted()

    fun all(): List<Agent> = byName.values.toList()
}
