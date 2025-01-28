package ch.fortidemo.webapp_adfc;

import ch.fortidemo.grpcvideoserver.VideoServerApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = VideoServerApplication.class) // Explicitly set the application class
final class VideoServerApplicationTests {

	@Test
	void contextLoads() {
		// This test ensures the Spring Boot application context starts successfully
	}
}