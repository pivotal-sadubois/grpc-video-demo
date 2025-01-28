package ch.fortidemo.grpcvideoserver;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Service
public class GrpcServer {

    private final Server server;

    public GrpcServer(VideoServiceImpl videoService) {
        this.server = NettyServerBuilder.forPort(9090)
                .addService(videoService)
                .maxInboundMessageSize(10 * 1024 * 1024) // Limit max request size to 10MB
                .flowControlWindow(16 * 1024 * 1024) // Optimize memory for large streaming
                .permitKeepAliveWithoutCalls(true)
                .permitKeepAliveTime(10, TimeUnit.SECONDS)
                .build();
    }

    public void start() throws IOException, InterruptedException {
        System.out.println("✅ gRPC Video Server is running on port 9090...");
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("🔻 Shutting down gRPC server...");
            try {
                server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }));

        // ✅ Keep the server running
        server.awaitTermination();
    }
}