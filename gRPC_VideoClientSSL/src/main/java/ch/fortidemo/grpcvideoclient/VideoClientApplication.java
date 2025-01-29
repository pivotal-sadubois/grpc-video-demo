package ch.fortidemo.grpcvideoclient;

import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.StreamObserver;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;
import ch.fortidemo.grpc.video.VideoServiceGrpc;
import ch.fortidemo.grpc.video.VideoRequest;
import ch.fortidemo.grpc.video.VideoChunk;
import org.springframework.core.io.ClassPathResource;

import java.io.*;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VideoClientApplication extends Application {
	private static final Logger LOGGER = Logger.getLogger(VideoClientApplication.class.getName());
	private static File tempFile;
	private static MediaPlayer mediaPlayer;
	private static boolean isPlaying = false;
	private static long totalBytesReceived = 0;
	private static String serverAddress = "localhost";
	private static int serverPort = 9090;
	private static OutputStream os;
	private static boolean isVideoReady = false;

	public static void main(String[] args) {
		if (args.length >= 2) {
			serverAddress = args[0];
			try {
				serverPort = Integer.parseInt(args[1]);
			} catch (NumberFormatException e) {
				LOGGER.log(Level.SEVERE, "Invalid port number. Using default: " + serverPort);
			}
		}

		LOGGER.info("🚀 Starting JavaFX application...");
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) {
		LOGGER.info("📡 Connecting to gRPC Server: " + serverAddress + ":" + serverPort);

		// Remove the initial empty player window issue
		primaryStage.close();

		new Thread(this::startGrpcClient).start();
	}

	private void startGrpcClient() {
		try {
			ClassPathResource trustCertResource = new ClassPathResource("certs/ca.crt");
			InputStream trustCertStream = trustCertResource.getInputStream();
			LOGGER.info("✅ TLS Certificate loaded successfully.");

			ManagedChannel channel = NettyChannelBuilder.forAddress(serverAddress, serverPort)
					.overrideAuthority("grpc-video-server-local")
					.sslContext(GrpcSslContexts.forClient().trustManager(trustCertStream).build())
					.build();

			VideoServiceGrpc.VideoServiceStub stub = VideoServiceGrpc.newStub(channel);
			VideoRequest request = VideoRequest.newBuilder().setFilename("video.mp4").build();

			tempFile = Files.createTempFile("streaming-video", ".mp4").toFile();
			LOGGER.info("📥 Downloading video to: " + tempFile.getAbsolutePath());

			os = new BufferedOutputStream(new FileOutputStream(tempFile), 10 * 1024 * 1024); // 10MB buffer

			stub.streamVideo(request, new StreamObserver<VideoChunk>() {
				@Override
				public void onNext(VideoChunk chunk) {
					try {
						byte[] data = chunk.getData().toByteArray();
						os.write(data);
						os.flush();
						totalBytesReceived += data.length;

						// Start playback after 10MB is received
						if (!isPlaying && totalBytesReceived >= 10 * 1024 * 1024) {
							isPlaying = true;
							isVideoReady = true;
							Platform.runLater(() -> playVideo(tempFile));
						}
					} catch (IOException e) {
						LOGGER.log(Level.SEVERE, "Error writing video chunk", e);
					}
				}

				@Override
				public void onError(Throwable t) {
					LOGGER.log(Level.SEVERE, "❌ gRPC Connection Error: " + t.getMessage(), t);
				}

				@Override
				public void onCompleted() {
					try {
						os.close();
						LOGGER.info("✅ Video download complete!");
						channel.shutdown();
					} catch (IOException e) {
						LOGGER.log(Level.SEVERE, "Error closing stream", e);
					}
				}
			});

		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "❌ Exception in gRPC client", e);
		}
	}

	private void playVideo(File videoFile) {
		if (!isVideoReady) {
			LOGGER.warning("🚨 Video is not ready yet! Waiting for more data...");
			return;
		}

		Platform.runLater(() -> {
			try {
				LOGGER.info("🎬 Starting playback of: " + videoFile.getAbsolutePath());

				Media media = new Media(videoFile.toURI().toString());
				mediaPlayer = new MediaPlayer(media);
				MediaView mediaView = new MediaView(mediaPlayer);

				StackPane root = new StackPane();
				root.getChildren().add(mediaView);

				Scene scene = new Scene(root, 800, 600);
				Stage stage = new Stage();
				stage.setTitle("gRPC Video Player");
				stage.setScene(scene);
				stage.show();

				mediaPlayer.play();
				LOGGER.info("▶ Video playback started...");
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "❌ Video playback error", e);
			}
		});
	}
}