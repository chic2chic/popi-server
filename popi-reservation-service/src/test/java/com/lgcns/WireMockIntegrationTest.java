package com.lgcns;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureWireMock(port = 0)
public abstract class WireMockIntegrationTest {

    @Autowired protected WireMockServer wireMockServer;

    @BeforeEach
    void restartWireMock() {
        wireMockServer.stop();
        wireMockServer.start();
    }

    @AfterEach
    void resetWireMock() {
        wireMockServer.resetAll();
    }
}
