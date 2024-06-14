package dev.EduVisor.EduVisorAI.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties
@Configuration
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ArtChatProperties {
    @Value("${serpapi-api-key}")
    private String SERPAPI_API_KEY;
}
