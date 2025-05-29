package com.lgcns.infra.properties;

import com.lgcns.infra.iamport.IamportProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties({IamportProperties.class})
@Configuration
public class PropertiesConfig {}
