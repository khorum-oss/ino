package org.khorum.oss.ino.test

import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import java.nio.file.Files
import java.nio.file.Path

/**
 * Per-test `INO_HOME` isolation.
 *
 *  - `beforeEach` creates a fresh tempdir and points `ino.home` at it.
 *  - `afterEach` clears the property and recursively deletes the tempdir.
 *
 * Tests can read [homeDir] to resolve files inside the sandbox (e.g. the
 * SQLite database path used by repository slice tests).
 */
class InoHomeExtension : BeforeEachCallback, AfterEachCallback {

    lateinit var homeDir: Path
        private set

    override fun beforeEach(context: ExtensionContext) {
        homeDir = Files.createTempDirectory("ino-home-")
        System.setProperty(PROPERTY, homeDir.toString())
    }

    override fun afterEach(context: ExtensionContext) {
        System.clearProperty(PROPERTY)
        homeDir.toFile().deleteRecursively()
    }

    companion object {
        const val PROPERTY = "ino.home"
    }
}
