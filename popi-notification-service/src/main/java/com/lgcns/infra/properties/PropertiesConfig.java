package com.lgcns.infra.properties;

import com.lgcns.infra.firebase.FcmProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties({FcmProperties.class})
@Configuration
public class PropertiesConfig {}
