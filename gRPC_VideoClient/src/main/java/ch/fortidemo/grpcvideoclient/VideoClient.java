package ch.fortidemo.grpcvideoclient;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
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
import java.io.*;

public class VideoClient extends Application {
    private static PipedInputStream pipedInputStream;
    private static PipedOutputStream pipedOutputStream;
    private static ManagedChannel channel;
    private static File streamingFile;

    // Default values
    private static String serverAddress = "localhost";
    private static int serverPort = 9090;

    public static void main(String[] args) {
        System.out.println("\n🚀 [DEBUG] Starting VideoClient...");
        System.out.println("📝 [DEBUG] Received CLI arguments:");
        for (int i = 0; i < args.length; i++) {
            System.out.println("   🔹 Arg[" + i + "]: " + args[i]);
        }

        // Read CLI arguments and apply them
        if (args.length >= 2) {
            serverAddress = args[0];
            try {
                serverPort = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("❌ [ERROR] Invalid port number provided. Using default: " + serverPort);
            }
        }

        System.out.println("📡 [DEBUG] Using gRPC connection: " + serverAddress + ":" + serverPort);

        // JavaFX requires a separate thread
        new Thread(() -> Application.launch(VideoClient.class, args)).start();
    }

    @Override
    public void start(Stage primaryStage) {
        System.out.println("\n🎬 [DEBUG] JavaFX started. Checking CLI parameters...");

        Parameters params = getParameters();
        if (!params.getRaw().isEmpty()) {
            System.out.println("📌 [DEBUG] JavaFX received CLI arguments:");
            for (int i = 0; i < params.getRaw().size(); i++) {
                System.out.println("   🔹 JavaFX Arg[" + i + "]: " + params.getRaw().get(i));
            }
        }

        if (params.getRaw().size() >= 2) {
            serverAddress = params.getRaw().get(0);
            try {
                serverPort = Integer.parseInt(params.getRaw().get(1));
            } catch (NumberFormatException e) {
                System.err.println("❌ [ERROR] JavaFX received invalid port number. Using default: " + serverPort);
            }
        }

        System.out.println("📡 [DEBUG] JavaFX final gRPC connection: " + serverAddress + ":" + serverPort);

        // Start gRPC client AFTER JavaFX initializes
        new Thread(() -> {
            try {
                startGrpcClient();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void startGrpcClient() throws Exception {
        System.out.println("\n📡 [DEBUG] Connecting to gRPC server at " + serverAddress + ":" + serverPort);

        channel = ManagedChannelBuilder.forAddress(serverAddress, serverPort)
                .usePlaintext()
                .build();

        VideoServiceGrpc.VideoServiceStub stub = VideoServiceGrpc.newStub(channel);
        VideoRequest request = VideoRequest.newBuilder().setFilename("video.mp4").build();

        streamingFile = File.createTempFile("streaming-video", ".mp4");
        System.out.println("\n📥 [DEBUG] Streaming video to: " + streamingFile.getAbsolutePath());

        // Write the piped stream to a temporary file
        new Thread(() -> {
            try (FileOutputStream fileOutputStream = new FileOutputStream(streamingFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = pipedInputStream.read(buffer)) != -1) {
                    fileOutputStream.write(buffer, 0, bytesRead);
                }
                fileOutputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        stub.streamVideo(request, new StreamObserver<VideoChunk>() {
            @Override
            public void onNext(VideoChunk chunk) {
                try {
                    pipedOutputStream.write(chunk.getData().toByteArray());
                    pipedOutputStream.flush();
                    System.out.println("\n📦 [DEBUG] Streaming chunk: " + chunk.getData().size() + " bytes");

                    if (streamingFile.length() > 4096) {
                        Platform.runLater(() -> playVideo(streamingFile));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("\n❌ [ERROR] gRPC Error: " + t.getMessage());
                t.printStackTrace();
            }

            @Override
            public void onCompleted() {
                try {
                    pipedOutputStream.close();
                    channel.shutdown();
                    System.out.println("\n✅ [DEBUG] Streaming complete!");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static void playVideo(File videoFile) {
        System.out.println("\n📺 [DEBUG] Playing video...");

        Stage stage = new Stage();
        Media media = new Media(videoFile.toURI().toString());
        MediaPlayer mediaPlayer = new MediaPlayer(media);
        MediaView mediaView = new MediaView(mediaPlayer);

        StackPane root = new StackPane();
        root.getChildren().add(mediaView);

        Scene scene = new Scene(root, 800, 600);
        stage.setTitle("gRPC Video Player");
        stage.setScene(scene);
        stage.show();

        mediaPlayer.play();
    }
}