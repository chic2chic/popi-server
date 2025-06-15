package com.lgcns.service.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(GrpcClientTestConfig.class)
public abstract class GrpcIntegrationTest {

    @Autowired private InMemoryGrpcServer inMemoryGrpcServer;

    @BeforeEach
    void setup() throws Exception {
        inMemoryGrpcServer.start(new TestAuthGrpcService());
    }

    @AfterEach
    void tearDown() {
        inMemoryGrpcServer.shutdown();
    }
}
