package com.r3edge.minilock.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "com.r3edge.minilock")
@EntityScan(basePackages = "com.r3edge.minilock")
public class MiniLockAutoConfiguration {}
