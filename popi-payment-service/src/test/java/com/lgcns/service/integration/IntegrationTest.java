package com.lgcns.service.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureWireMock(port = 0)
@Import(GrpcClientTestConfig.class)
public abstract class IntegrationTest {

    @Autowired private WireMockServer wireMockServer;
    @Autowired private InMemoryGrpcServer inMemoryGrpcServer;

    @BeforeEach
    void setUp() throws IOException {
        wireMockServer.stop();
        wireMockServer.start();

        inMemoryGrpcServer.start(new TestMemberGrpcService());
    }

    @AfterEach
    void afterEach() {
        wireMockServer.resetAll();

        inMemoryGrpcServer.shutdown();
    }
}
