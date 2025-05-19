package com.lgcns.infra.properties;

import com.lgcns.infra.jwt.JwtProperties;
import com.lgcns.infra.oidc.OidcProperties;
import com.lgcns.infra.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties({RedisProperties.class, JwtProperties.class, OidcProperties.class})
@Configuration
public class PropertiesConfig {}
