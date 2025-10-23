package com.mikesajak.ebooklib

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class EbooklibApplication

fun main(args: Array<String>) {
	runApplication<EbooklibApplication>(*args)
}
