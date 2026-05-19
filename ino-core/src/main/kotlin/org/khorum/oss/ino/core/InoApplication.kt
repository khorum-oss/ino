package org.khorum.oss.ino.core

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class InoApplication

fun main(args: Array<String>) {
    runApplication<InoApplication>(*args)
}
