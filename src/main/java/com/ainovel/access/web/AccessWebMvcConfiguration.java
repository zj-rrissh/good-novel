package com.ainovel.access.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AccessWebMvcConfiguration implements WebMvcConfigurer {

    private final AccessHeaderInterceptor accessHeaderInterceptor;

    public AccessWebMvcConfiguration(AccessHeaderInterceptor accessHeaderInterceptor) {
        this.accessHeaderInterceptor = accessHeaderInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(accessHeaderInterceptor)
                .addPathPatterns("/api/**");
    }
}
