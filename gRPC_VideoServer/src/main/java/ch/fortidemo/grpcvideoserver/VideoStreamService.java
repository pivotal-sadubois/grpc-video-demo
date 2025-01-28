package ch.fortidemo.grpcvideoserver;

import io.grpc.stub.StreamObserver;
import ch.fortidemo.grpc.video.VideoRequest;
import ch.fortidemo.grpc.video.VideoChunk;
import ch.fortidemo.grpc.video.VideoServiceGrpc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class VideoStreamService extends VideoServiceGrpc.VideoServiceImplBase {

    private static final String VIDEO_PATH = "src/main/resources/video.mp4";
    private static final int CHUNK_SIZE = 64 * 1024; // 64KB

    @Override
    public void streamVideo(VideoRequest request, StreamObserver<VideoChunk> responseObserver) {
        File file = new File(VIDEO_PATH);
        if (!file.exists()) {
            responseObserver.onError(new RuntimeException("File not found: " + VIDEO_PATH));
            return;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[CHUNK_SIZE];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                VideoChunk chunk = VideoChunk.newBuilder()
                        .setData(com.google.protobuf.ByteString.copyFrom(buffer, 0, bytesRead))
                        .build();
                responseObserver.onNext(chunk);
            }
            responseObserver.onCompleted();
        } catch (IOException e) {
            responseObserver.onError(e);
        }
    }
}
