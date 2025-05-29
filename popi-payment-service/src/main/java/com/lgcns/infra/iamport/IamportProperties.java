package com.lgcns.infra.iamport;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "iamport")
public record IamportProperties(String key, String secret) {}
