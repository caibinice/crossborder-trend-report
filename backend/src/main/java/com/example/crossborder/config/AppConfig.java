package com.example.crossborder.config;
import com.example.crossborder.repository.AdminDataRepository;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
@Configuration
@EnableConfigurationProperties({ReportProperties.class, SourceProperties.class, AiProperties.class, SecurityProperties.class, BootstrapProperties.class})
public class AppConfig implements WebMvcConfigurer {
  @Value("${app.cors.allowed-origins}") private String allowedOrigins;
  @Override public void addCorsMappings(CorsRegistry registry){
    registry.addMapping("/api/**").allowedOrigins(Arrays.stream(allowedOrigins.split(",")).map(String::trim).toArray(String[]::new)).allowedMethods("GET","POST","PUT","DELETE","OPTIONS").allowedHeaders("*");
  }

  @Bean
  ApplicationRunner initializeDatabase(AdminDataRepository adminData) {
    return args -> adminData.ensureSeedData();
  }
}

