package ch.fortidemo.grpcvideoclient;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
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

	// Default values
	private static String serverAddress = "localhost";
	private static int serverPort = 9090;

	public static void main(String[] args) {
		System.out.println("\n🚀 [DEBUG] Starting VideoClientApplication...");
		System.out.println("📝 [DEBUG] Received CLI arguments:");
		for (int i = 0; i < args.length; i++) {
			System.out.println("   🔹 Arg[" + i + "]: " + args[i]);
		}

		// Read CLI arguments
		if (args.length >= 2) {
			serverAddress = args[0];
			try {
				serverPort = Integer.parseInt(args[1]);
			} catch (NumberFormatException e) {
				System.err.println("❌ [ERROR] Invalid port number provided. Using default: " + serverPort);
			}
		}

		System.out.println("📡 [DEBUG] Using gRPC connection: " + serverAddress + ":" + serverPort);

		System.out.println("🚀 Starting JavaFX...");
		Platform.startup(() -> startGrpcClient());
	}

	private static void startGrpcClient() {
		System.out.println("📡 Connecting to gRPC server: " + serverAddress + ":" + serverPort);

		ManagedChannel channel = ManagedChannelBuilder.forAddress(serverAddress, serverPort)
				.usePlaintext()
				.build();

		VideoServiceGrpc.VideoServiceStub stub = VideoServiceGrpc.newStub(channel);
		VideoRequest request = VideoRequest.newBuilder().setFilename("video.mp4").build();

		try {
			tempFile = File.createTempFile("video", ".mp4");
			System.out.println("📥 Downloading video to: " + tempFile.getAbsolutePath());

			OutputStream os = new FileOutputStream(tempFile);

			stub.streamVideo(request, new StreamObserver<VideoChunk>() {
				private long totalBytesReceived = 0;

				@Override
				public void onNext(VideoChunk chunk) {
					try {
						os.write(chunk.getData().toByteArray());
						totalBytesReceived += chunk.getData().size();
						System.out.printf("📦 [DEBUG] Received chunk: %d bytes (%.2f MB total)\n",
								chunk.getData().size(), totalBytesReceived / (1024.0 * 1024.0));

						// Start playing once we receive the first chunk
						if (!isPlaying && totalBytesReceived > 1024 * 1024) { // Start playback after ~1MB
							isPlaying = true;
							Platform.runLater(() -> VideoClient.playVideo(tempFile));
						}
					} catch (Exception e) {
						System.err.println("❌ [ERROR] Error writing chunk: " + e.getMessage());
						e.printStackTrace();
					}
				}

				@Override
				public void onError(Throwable t) {
					System.err.println("❌ [ERROR] gRPC Connection Error: " + t.getMessage());
					t.printStackTrace();
				}

				@Override
				public void onCompleted() {
					try {
						os.close();
						System.out.println("✅ [DEBUG] Video download complete!");
						channel.shutdown();
					} catch (Exception e) {
						System.err.println("❌ [ERROR] Error closing output stream: " + e.getMessage());
						e.printStackTrace();
					}
				}
			});

		} catch (Exception e) {
			System.err.println("❌ [ERROR] Exception in gRPC client: " + e.getMessage());
			e.printStackTrace();
		}
	}
}