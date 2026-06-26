package com.abdude.java_spring_boot.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;


@Configuration
@EnableJpaRepositories("com.abdude.java_spring_boot.repository")
@EnableTransactionManagement
public class DomainConfig {
}
