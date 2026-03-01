package com.mikesajak.ebooklib

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
@EnableAsync
class EbooklibApplication

fun main(args: Array<String>) {
	runApplication<EbooklibApplication>(*args)
}
