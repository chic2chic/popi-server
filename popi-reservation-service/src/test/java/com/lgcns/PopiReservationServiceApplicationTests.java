package com.lgcns;

import com.lgcns.service.integration.GrpcClientTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(GrpcClientTestConfig.class)
class PopiReservationServiceApplicationTests {

    @Test
    void contextLoads() {}
}
