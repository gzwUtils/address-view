package kd.address.view.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Browser API traffic is same-origin through the frontend /api proxy.
        // No cross-origin mapping is registered for credentialed endpoints.
    }
}
