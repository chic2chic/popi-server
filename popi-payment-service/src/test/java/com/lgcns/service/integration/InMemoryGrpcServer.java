package com.lgcns.service.integration;

import static com.lgcns.service.integration.GrpcTestConstants.SERVER_NAME;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import org.springframework.stereotype.Component;

@Component
public class InMemoryGrpcServer {

    private Server server;

    public void start(BindableService... services) throws IOException {
        InProcessServerBuilder builder =
                InProcessServerBuilder.forName(SERVER_NAME).directExecutor();
        for (BindableService service : services) {
            builder.addService(service);
        }
        server = builder.build().start();
    }
}
