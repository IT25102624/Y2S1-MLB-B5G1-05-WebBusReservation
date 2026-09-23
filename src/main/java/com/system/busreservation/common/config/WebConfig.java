package com.system.busreservation.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/search.html");
        registry.addViewController("").setViewName("forward:/search.html");
        registry.addViewController("/search-view").setViewName("forward:/search.html");
        registry.addViewController("/auth-portal").setViewName("forward:/admin.html");
        registry.addViewController("/admin-console").setViewName("forward:/admin.html");
    }
}