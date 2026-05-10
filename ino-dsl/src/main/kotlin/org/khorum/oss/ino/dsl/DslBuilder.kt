package org.khorum.oss.ino.dsl

import org.khorum.oss.konstellation.metaDsl.CoreDslBuilder

/**
 * Marker interface every konstellation-generated builder in `ino-dsl` will implement.
 *
 * Tagging it with `@InoDsl` propagates the DSL scope to all generated builders so
 * compile-time scope checking works for nested DSLs (`agent { tool { ... } }`).
 */
@InoDsl
interface DslBuilder<T> : CoreDslBuilder<T>
