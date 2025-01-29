package ch.fortidemo.grpcvideoserver;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.ClientAuth;
import io.grpc.netty.shaded.io.netty.channel.epoll.Epoll;
import io.grpc.netty.shaded.io.netty.channel.nio.NioEventLoopGroup;
import io.grpc.netty.shaded.io.netty.channel.epoll.EpollEventLoopGroup;
import io.grpc.netty.shaded.io.netty.channel.socket.nio.NioServerSocketChannel;
import io.grpc.netty.shaded.io.netty.channel.epoll.EpollServerSocketChannel;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.io.InputStream;

@Service
public class GrpcServer {
    private Server server;
    private final VideoServiceImpl videoService;

    public GrpcServer(VideoServiceImpl videoService) {
        this.videoService = videoService;
    }

    public void start() throws IOException {
        System.setProperty("io.grpc.internal.DontEnableCensusStats", "true");
        System.setProperty("io.grpc.internal.DontEnableCensusTracing", "true");
        System.out.println("🔐 Starting gRPC Server with TLS...");

        // 🚀 Force Netty to use NIO on macOS to prevent Epoll issues
        if (!isLinux()) {
            System.setProperty("io.grpc.netty.shaded.io.netty.transport.noNative", "true");
        }

        // 🚀 Disable gRPC Census Tracing to prevent missing class issues
        System.setProperty("io.grpc.internal.DontEnableTracing", "true");

        // ✅ Load TLS certificates
        Resource certResource = new ClassPathResource("certs/server.crt");
        Resource keyResource = new ClassPathResource("certs/server.key");
        Resource caCertResource = new ClassPathResource("certs/ca.crt"); // Optional for mTLS

        try (InputStream certChainStream = certResource.getInputStream();
             InputStream privateKeyStream = keyResource.getInputStream();
             InputStream trustCertStream = caCertResource.getInputStream()) {

            // ✅ Use SSL context with InputStreams
            SslContext sslContext = GrpcSslContexts.forServer(certChainStream, privateKeyStream)
                    .trustManager(trustCertStream) // Trust CA (for mTLS)
                    .clientAuth(ClientAuth.NONE) // Set to REQUIRED for mTLS
                    .protocols("TLSv1.2") // Ensure compatibility
                    .build();

            // ✅ Select transport dynamically
            NettyServerBuilder serverBuilder = createServerBuilder(9090)
                    .sslContext(sslContext)
                    .addService(videoService);

            server = serverBuilder.build();
            server.start();

            System.out.println("✅ gRPC Server is running with TLS on port 9090...");

            // ✅ Handle server termination properly
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("🔻 Shutting down gRPC Server...");
                GrpcServer.this.stop();
            }));

            try {
                server.awaitTermination();
            } catch (InterruptedException e) {
                System.err.println("⛔ gRPC Server interrupted: " + e.getMessage());
                Thread.currentThread().interrupt();
            }
        } catch (SSLException e) {
            System.err.println("❌ [ERROR] SSL Exception: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    private NettyServerBuilder createServerBuilder(int port) {
        if (isLinux() && Epoll.isAvailable()) {
            System.out.println("🐧 Using Epoll for gRPC transport on Linux...");
            return NettyServerBuilder.forPort(port)
                    .channelType(EpollServerSocketChannel.class)
                    .bossEventLoopGroup(new EpollEventLoopGroup())
                    .workerEventLoopGroup(new EpollEventLoopGroup());
        } else {
            System.out.println("🍏 Using NIO for gRPC transport on macOS...");
            return NettyServerBuilder.forPort(port)
                    .channelType(NioServerSocketChannel.class)
                    .bossEventLoopGroup(new NioEventLoopGroup())
                    .workerEventLoopGroup(new NioEventLoopGroup());
        }
    }

    private boolean isLinux() {
        return System.getProperty("os.name").toLowerCase().contains("linux");
    }
}