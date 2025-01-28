package ch.fortidemo.grpcvideoserver;

import io.grpc.stub.StreamObserver;
import ch.fortidemo.grpc.video.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class VideoServiceImpl extends VideoServiceGrpc.VideoServiceImplBase {

    private final String videoFilePath;
    private final String defaultFileName;

    public VideoServiceImpl(
            @Value("${video.file.path:/data/videos}") String videoFilePath,
            @Value("${video.file.name:video.mp4}") String defaultFileName) {
        this.videoFilePath = videoFilePath;
        this.defaultFileName = defaultFileName;

        System.out.println("✅ VideoServiceImpl initialized with:");
        System.out.println("📂 videoFilePath: " + videoFilePath);
        System.out.println("🎬 defaultFileName: " + defaultFileName);
    }

    @Override
    public void streamVideo(VideoRequest request, StreamObserver<VideoChunk> responseObserver) {
        String filename = request.getFilename().isEmpty() ? defaultFileName : request.getFilename();
        File videoFile = new File(videoFilePath, filename);

        if (!videoFile.exists()) {
            System.err.println("❌ Video file not found: " + filename);
            responseObserver.onError(new IOException("File not found: " + filename));
            return;
        }

        System.out.println("📂 Streaming video file: " + videoFile.getAbsolutePath());

        try (FileInputStream fis = new FileInputStream(videoFile)) {
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {
                VideoChunk chunk = VideoChunk.newBuilder()
                        .setData(com.google.protobuf.ByteString.copyFrom(buffer, 0, bytesRead))
                        .build();
                responseObserver.onNext(chunk);
                System.out.println("📦 Sent chunk of size: " + bytesRead + " bytes");
            }

            System.out.println("✅ Video streaming completed: " + filename);
            responseObserver.onCompleted();
        } catch (IOException e) {
            System.err.println("❌ Error while streaming: " + e.getMessage());
            responseObserver.onError(e);
        }
    }
}