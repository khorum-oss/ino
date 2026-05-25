package org.khorum.oss.ino.core.config

import org.khorum.oss.ino.core.util.UuidV7
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class ClockConfig {

    @Bean
    fun clock(): Clock = Clock.systemUTC()

    @Bean
    fun uuidV7(clock: Clock): UuidV7 = UuidV7(clock)
}
