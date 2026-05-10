package org.khorum.oss.ino.dsl

import org.khorum.oss.konstellation.metaDsl.annotation.GeneratedDsl
import org.khorum.oss.konstellation.metaDsl.annotation.defaults.state.standard.DefaultEmptyString
import org.khorum.oss.konstellation.metaDsl.annotation.defaults.state.standard.DefaultFalse

/**
 * JSON Schema primitive type tag. Lives in conjunction with the
 * `ParameterTypeSpec` sealed hierarchy: this enum is the fast,
 * easy-to-compare label, while the sealed hierarchy carries any
 * type-specific structure.
 *
 * Values mirror the JSON Schema spec so renderers in `ino-core` can emit
 * `"type": "string"` etc. directly.
 */
enum class ParameterType { STRING, NUMBER, INTEGER, BOOLEAN, ARRAY, OBJECT }

/**
 * Structural representation of a parameter's type.
 *
 * Each variant exposes its `type: ParameterType` tag for cheap dispatch,
 * but exhaustive `when` over the sealed hierarchy is also available when
 * type-specific behavior is needed
 *
 * MVP ships only the four primitive variants. Nested arrays and objects
 * are explicitly deferred — see the commented-out variants below.
 */
sealed interface ParameterTypeSpec {
    val type: ParameterType

    data object StringSpec : ParameterTypeSpec {
        override val type = ParameterType.STRING
    }

    data object NumberSpec : ParameterTypeSpec {
        override val type = ParameterType.NUMBER
    }

    data object IntegerSpec : ParameterTypeSpec {
        override val type = ParameterType.INTEGER
    }

    data object BooleanSpec : ParameterTypeSpec {
        override val type = ParameterType.BOOLEAN
    }

    data class ArraySpec(val elementTypeSpec: ParameterTypeSpec) : ParameterTypeSpec {
        override val type = ParameterType.ARRAY
    }

    data class ObjectSpec(val properties: List<ToolParameter> = emptyList()) : ParameterTypeSpec {
        override val type = ParameterType.OBJECT
    }
}

/**
 * One parameter of a `Tool`. Used as the element type of `Tool.parameters`
 * (declared with `@ListDsl` so konstellation generates a nested DSL block):
 *
 *     tool("web_search") {
 *         description = "Search the web."
 *         parameters {
 *             toolParameter {
 *                 name = "query"
 *                 typeSpec = ParameterTypeSpec.StringSpec
 *                 description = "Search query string."
 *                 required()
 *             }
 *             toolParameter {
 *                 name = "limit"
 *                 typeSpec = ParameterTypeSpec.IntegerSpec
 *             }
 *         }
 *     }
 *
 * (With `import org.khorum.oss.ino.dsl.ParameterTypeSpec.*` the call site
 *  shortens to `typeSpec = StringSpec`.)
 *
 * At runtime, `ino-core` walks an Agent's `tools` and renders these into
 * provider-specific JSON Schemas (Anthropic, OpenAI, and Ollama all want
 * slightly different shapes). That rendering is **not** this class's job —
 * keeping it pure data means the same declarations work across every provider.
 */
@GeneratedDsl
@Enhancement(
    "string regex/format/minLength/maxLength; integer & number min/max; " +
        "enum values; default values; nested array/object schemas (ArraySpec / ObjectSpec)"
)
data class ToolParameter(
    val name: String,
    val typeSpec: ParameterTypeSpec,
    @DefaultEmptyString
    val description: String = "",
    @DefaultFalse
    val required: Boolean = false,
)
