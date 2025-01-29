package ch.fortidemo.grpcvideoserver;

import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import ch.fortidemo.grpc.video.*;

@Service
public class VideoServiceImpl extends VideoServiceGrpc.VideoServiceImplBase {
    private final String videoFilePath;
    private final String defaultFileName;
    private static final int CHUNK_SIZE = 64 * 1024; // 64 KB
    private final long fileSizeBytes;
    private final double fileSizeMB;

    public VideoServiceImpl(
            @Value("${VIDEO_FILE_PATH:/root/video/}") String videoFilePath,
            @Value("${VIDEO_FILE_NAME:video.mp4}") String defaultFileName) {
        this.videoFilePath = videoFilePath;
        this.defaultFileName = defaultFileName;

        // Calculate file size once at server startup
        File videoFile = new File(videoFilePath, defaultFileName);
        if (videoFile.exists()) {
            this.fileSizeBytes = videoFile.length();
            this.fileSizeMB = fileSizeBytes / (1024.0 * 1024.0);
            System.out.printf("✅ VideoServiceImpl initialized with file: %s (%.2f MB)%n", videoFile.getAbsolutePath(), fileSizeMB);
        } else {
            this.fileSizeBytes = 0;
            this.fileSizeMB = 0;
            System.err.println("❌ Error: Video file not found at startup: " + videoFile.getAbsolutePath());
        }
    }

    @Override
    public void streamVideo(VideoRequest request, StreamObserver<VideoChunk> responseObserver) {
        File videoFile = new File(videoFilePath, defaultFileName);

        if (!videoFile.exists()) {
            System.err.println("❌ Video file not found: " + defaultFileName);
            responseObserver.onError(new IOException("File not found: " + defaultFileName));
            return;
        }

        System.out.printf("📂 Streaming video file: %s (Size: %.2f MB)%n", defaultFileName, fileSizeMB);

        long totalBytesSent = 0;
        long startTime = System.nanoTime(); // Start timer

        try (FileInputStream fis = new FileInputStream(videoFile)) {
            byte[] buffer = new byte[CHUNK_SIZE];
            int bytesRead;
            boolean firstChunkSent = false;

            while ((bytesRead = fis.read(buffer)) != -1) {
                totalBytesSent += bytesRead;
                double totalMB = totalBytesSent / (1024.0 * 1024.0);

                // Adjust buffer size for the last chunk
                byte[] exactSizeBuffer = new byte[bytesRead];
                System.arraycopy(buffer, 0, exactSizeBuffer, 0, bytesRead);

                VideoChunk chunk = VideoChunk.newBuilder()
                        .setData(com.google.protobuf.ByteString.copyFrom(exactSizeBuffer))
                        .build();
                responseObserver.onNext(chunk);

                if (!firstChunkSent) {
                    firstChunkSent = true;
                    System.out.println("⏱ First chunk sent, starting timer...");
                }

                System.out.printf("[DEBUG] Sent chunk: %d bytes (%.2f MB total / %.2f MB)%n",
                        bytesRead, totalMB, fileSizeMB);
            }

            double elapsedTime = (System.nanoTime() - startTime) / 1_000_000_000.0;
            System.out.printf("✅ Video streaming completed: %s in %.2f seconds%n",
                    defaultFileName, elapsedTime);

            responseObserver.onCompleted();
            System.gc(); // Run garbage collection only at the end
        } catch (IOException e) {
            System.err.println("❌ Error while streaming: " + e.getMessage());
            responseObserver.onError(e);
        }
    }
}