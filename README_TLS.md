# Securing gRPC Video Streaming with TLS

## **🔐 Overview**
This guide explains how to secure the **gRPC Video Server** and **gRPC Video Client** communication using **TLS encryption**. By the end of this guide, all traffic between the client and server will be encrypted and authenticated using X.509 certificates.

---

## **1️⃣ Generate TLS Certificates**
We need the following certificates:
- **Root CA certificate** (to sign server and client certs)
- **Server certificate & private key** (for `grpc-video-server`)
- **Client certificate & private key** (for `grpc-video-client`)

### **🔧 Generate Certificates with OpenSSL**
Run the following commands to generate self-signed certificates.

#### **Step 1: Create a Root Certificate Authority (CA)**
```sh
openssl genpkey -algorithm RSA -out ca.key
openssl req -x509 -new -nodes -key ca.key -sha256 -days 365 -out ca.crt -subj "/CN=My gRPC CA"
```

#### **Step 2: Generate Server Certificate**
```sh
openssl genpkey -algorithm RSA -out server.key
openssl req -new -key server.key -out server.csr -subj "/CN=grpc-video-server"
openssl x509 -req -in server.csr -CA ca.crt -CAkey ca.key -CAcreateserial -out server.crt -days 365 -sha256
```

#### **Step 3: Generate Client Certificate**
```sh
openssl genpkey -algorithm RSA -out client.key
openssl req -new -key client.key -out client.csr -subj "/CN=grpc-video-client"
openssl x509 -req -in client.csr -CA ca.crt -CAkey ca.key -CAcreateserial -out client.crt -days 365 -sha256
```

Now you have:
- **Root CA:** `ca.crt`
- **Server:** `server.crt`, `server.key`
- **Client:** `client.crt`, `client.key`

---

## **2️⃣ Update gRPC Server (`GrpcServer.java`)**
Modify the **gRPC Video Server** to use TLS.

### **✅ `GrpcServer.java`**
```java
package ch.fortidemo.grpcvideoserver;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import java.io.File;
import java.io.IOException;
import org.springframework.stereotype.Service;

@Service
public class GrpcServer {
    private Server server;

    public void start() throws IOException {
        System.out.println("🔐 Starting gRPC Server with TLS...");

        // Load TLS certificates
        File certChainFile = new File("certs/server.crt");
        File privateKeyFile = new File("certs/server.key");

        // Build server with TLS
        server = NettyServerBuilder.forPort(9090)
                .sslContext(GrpcSslContexts.forServer(certChainFile, privateKeyFile).build())
                .addService(new VideoServiceImpl())
                .build()
                .start();

        System.out.println("✅ gRPC Server is running with TLS on port 9090...");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("🔻 Shutting down gRPC Server...");
            GrpcServer.this.stop();
        }));
    }

    public void stop() {
        if (server != null) {
            server.shutdown();
        }
    }
}
```

---

## **3️⃣ Update gRPC Client (`VideoClientApplication.java`)**
Modify the **gRPC Video Client** to use TLS and verify the server certificate.

### **✅ `VideoClientApplication.java`**
```java
package ch.fortidemo.grpcvideoclient;

import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import javafx.application.Platform;
import ch.fortidemo.grpc.video.VideoServiceGrpc;
import ch.fortidemo.grpc.video.VideoRequest;
import ch.fortidemo.grpc.video.VideoChunk;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class VideoClientApplication {
    private static File tempFile;
    private static boolean isPlaying = false;
    private static String serverAddress = "localhost";
    private static int serverPort = 9090;

    public static void main(String[] args) {
        System.out.println("🚀 Starting VideoClientApplication with TLS...");

        if (args.length >= 2) {
            serverAddress = args[0];
            try {
                serverPort = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("❌ [ERROR] Invalid port number. Using default: " + serverPort);
            }
        }

        System.out.println("📡 Connecting to gRPC Server: " + serverAddress + ":" + serverPort);
        Platform.startup(() -> startGrpcClient());
    }

    private static void startGrpcClient() {
        try {
            File trustCertCollectionFile = new File("certs/ca.crt");

            ManagedChannel channel = NettyChannelBuilder.forAddress(serverAddress, serverPort)
                    .sslContext(GrpcSslContexts.forClient().trustManager(trustCertCollectionFile).build())
                    .build();

            VideoServiceGrpc.VideoServiceStub stub = VideoServiceGrpc.newStub(channel);
            VideoRequest request = VideoRequest.newBuilder().setFilename("video.mp4").build();

            tempFile = File.createTempFile("video", ".mp4");
            System.out.println("📥 Downloading video to: " + tempFile.getAbsolutePath());

            OutputStream os = new FileOutputStream(tempFile);

            stub.streamVideo(request, new StreamObserver<VideoChunk>() {
                @Override
                public void onNext(VideoChunk chunk) {
                    try {
                        os.write(chunk.getData().toByteArray());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onError(Throwable t) {
                    System.err.println("❌ [ERROR] gRPC TLS Connection Error: " + t.getMessage());
                }

                @Override
                public void onCompleted() {
                    try {
                        os.close();
                        channel.shutdown();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

        } catch (Exception e) {
            System.err.println("❌ [ERROR] Exception in gRPC client: " + e.getMessage());
        }
    }
}
```

---

## **🎯 Final Steps**
### **Start the Secure gRPC Server**
```sh
java -jar target/grpc-video-server.jar
```

### **Start the Secure gRPC Client**
```sh
java -jar target/grpc-video-client.jar 10.0.10.122 9090
```

---

## **🚀 Conclusion**
Now your **gRPC Video Streaming** is fully **encrypted with TLS** 🔐. 🎉


