package org.khorum.oss.ino.dsl

/**
 * Marks a declaration that intentionally omits known-needed properties
 * (constraint fields, optional features, follow-up work) for the current
 * milestone, with the expectation that they'll be added later.
 *
 * Source-retention only — no runtime cost. Its purpose is to make deferred
 * work discoverable via grep / IDE search / static analysis without leaving
 * scattered TODOs in doc comments.
 *
 * Example:
 *
 *     @Enhancement("string regex/format/length, numeric min/max, enum values")
 *     data class ToolParameter(...)
 *
 * Note: this is a local stub for now. It belongs upstream in
 * konstellation-meta-dsl alongside `@DeprecatedDsl` etc., where any tooling
 * that walks DSL declarations can react to it consistently.
 */
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.VALUE_PARAMETER,
)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class Enhancement(val description: String)
