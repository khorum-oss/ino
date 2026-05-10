package org.khorum.oss.ino.dsl

import org.khorum.oss.konstellation.metaDsl.annotation.GeneratedDsl
import org.khorum.oss.konstellation.metaDsl.annotation.ListDsl
import org.khorum.oss.konstellation.metaDsl.annotation.RootDsl
import org.khorum.oss.konstellation.metaDsl.annotation.defaults.state.standard.DefaultEmptyList
import org.khorum.oss.konstellation.metaDsl.annotation.defaults.state.standard.DefaultEmptyString
import org.khorum.oss.konstellation.metaDsl.annotation.defaults.state.standard.DefaultFalse
import org.khorum.oss.konstellation.metaDsl.annotation.defaults.state.standard.NegationFunctionTemplate

/**
 * Declarative description of a tool an agent may call. Pure data — the actual
 * suspend execution body lives in a `ToolHandler` impl in an extension JAR
 * (registered via `META-INF/services` and looked up by `name`).
 */
@RootDsl
@GeneratedDsl
data class Tool(
    val name: String,
    @DefaultEmptyString
    val description: String = "",
    @ListDsl
    @DefaultEmptyList
    val parameters: List<ToolParameter> = emptyList(),

    val version: String? = null,
    @DefaultFalse(negationTemplate = NegationFunctionTemplate.IS_NOT)
    val dangerous: Boolean = false,
    val timeoutSeconds: Int? = null
)
