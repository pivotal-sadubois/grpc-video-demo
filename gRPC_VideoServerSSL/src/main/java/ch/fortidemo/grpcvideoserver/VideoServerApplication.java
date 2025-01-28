package ch.fortidemo.grpcvideoserver;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class VideoServerApplication implements CommandLineRunner {

	private final GrpcServer grpcServer;

	public VideoServerApplication(GrpcServer grpcServer) {
		this.grpcServer = grpcServer;
	}

	public static void main(String[] args) {
		SpringApplication.run(VideoServerApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		grpcServer.start();
	}
}