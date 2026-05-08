package io.github.chehsunliu.itx.backend.middleware;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

  private final RequireUserInterceptor requireUserInterceptor;

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry
        .addInterceptor(requireUserInterceptor)
        .addPathPatterns("/api/v1/posts/**", "/api/v1/users/**", "/api/v1/subscriptions/**");
  }
}
