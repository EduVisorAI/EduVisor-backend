package dev.EduVisor.EduVisorAI;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class EduVisorAiApplication {

	public static void main(String[] args) {
		SpringApplication.run(EduVisorAiApplication.class, args);
	}

}
