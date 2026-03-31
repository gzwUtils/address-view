package kd.address.view.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final long CORS_MAX_AGE_SECONDS = 3600L;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // TODO: restrict allowedOriginPatterns to specific domains in production
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Content-Type", "Authorization", "X-User-Id")
                .allowCredentials(false)
                .maxAge(CORS_MAX_AGE_SECONDS);
    }
}