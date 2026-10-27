package com.scicrop.biblia.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/**
 * Serves the recorded chapters (/audio/<edition>/<book>/<chapter>.m4a and .json) from a folder outside the jar.
 * Browsers revalidate every time (a cheap 304), since a regenerated chapter keeps its path.
 */
@Configuration
public class AudioConfig implements WebMvcConfigurer {

    private final String location;

    public AudioConfig(@Value("${biblia.audio.dir}") String dir) {
        this.location = Path.of(dir).toUri().toString();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/audio/**")
                .addResourceLocations(location.endsWith("/") ? location : location + "/")
                .setCacheControl(CacheControl.noCache().cachePublic());
    }
}
