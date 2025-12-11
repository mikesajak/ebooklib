package com.mikesajak.ebooklib.config

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration

@SpringBootTest
@ContextConfiguration(initializers = [TestcontainersConfig::class])
abstract class BaseIntegrationTest
