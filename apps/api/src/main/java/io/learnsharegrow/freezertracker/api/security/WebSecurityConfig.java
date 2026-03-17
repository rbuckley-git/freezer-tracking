package io.learnsharegrow.freezertracker.api.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebSecurityConfig implements WebMvcConfigurer {
  private final ApiKeyInterceptor apiKeyInterceptor;

  public WebSecurityConfig(ApiKeyInterceptor apiKeyInterceptor) {
    this.apiKeyInterceptor = apiKeyInterceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(apiKeyInterceptor);
  }
}
