package com.lgcns.infra.properties;

import com.lgcns.infra.firebase.FirebaseProperties;
import com.lgcns.infra.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties({FirebaseProperties.class, RedisProperties.class})
@Configuration
public class PropertiesConfig {}
