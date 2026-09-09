package com.projeto.gestao.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ExternalIntegrationProperties.class)
public class ExternalIntegrationConfiguration {
}
