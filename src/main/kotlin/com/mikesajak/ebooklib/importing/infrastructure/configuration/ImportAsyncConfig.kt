package com.mikesajak.ebooklib.importing.infrastructure.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.TaskExecutor
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

@Configuration
@EnableAsync
class ImportAsyncConfig {

    @Bean(name = ["importProcessingExecutor"])
    fun importProcessingExecutor(): TaskExecutor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 4
        executor.maxPoolSize = 8
        executor.queueCapacity = 500
        executor.setThreadNamePrefix("import-proc-")
        executor.initialize()
        return executor
    }
}
