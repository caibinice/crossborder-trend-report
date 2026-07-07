package com.example.crossborder.config;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
@Configuration
@EnableConfigurationProperties({ReportProperties.class, SourceProperties.class})
public class AppConfig implements WebMvcConfigurer {
  @Value("${app.cors.allowed-origins}") private String allowedOrigins;
  @Override public void addCorsMappings(CorsRegistry registry){
    registry.addMapping("/api/**").allowedOrigins(Arrays.stream(allowedOrigins.split(",")).map(String::trim).toArray(String[]::new)).allowedMethods("GET","POST","DELETE","OPTIONS").allowedHeaders("*");
  }
}

