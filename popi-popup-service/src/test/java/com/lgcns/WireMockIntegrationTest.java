package com.lgcns;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("test")
public abstract class WireMockIntegrationTest {

    @Autowired protected WireMockServer wireMockServer;

    @BeforeEach
    void setUp() {
        wireMockServer.stop();
        wireMockServer.start();
    }

    @AfterEach
    void afterEach() {
        wireMockServer.resetAll();
    }
}
