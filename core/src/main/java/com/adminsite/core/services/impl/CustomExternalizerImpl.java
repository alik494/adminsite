package com.adminsite.core.services.impl;

import com.adminsite.core.services.CustomExternalizer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Slf4j
@Component(service = CustomExternalizer.class)
@Designate(ocd = CustomExternalizerImpl.CustomExternalizerConfig.class)
public class CustomExternalizerImpl implements CustomExternalizer {

    public static final String HTML = ".html";
    public static final String SLASH = "/";

    private String externalHostName;
    private String externalScheme;
    private String contextPath;

    @ObjectClassDefinition(name = "Custom Externalizer Configuration")
    public @interface CustomExternalizerConfig {

        @AttributeDefinition(name = "External Hostname",
                description = "Hostname used for externalizing URLs.")
        String externalHostName() default "localhost:4502";

        @AttributeDefinition(name = "External Scheme",
                description = "Scheme used for externalizing URLs.")
        String externalScheme() default "http";

        @AttributeDefinition(name = "Context Path",
                description = "Context path for the externalized URLs, if required.")
        String contextPath() default "";
    }

    @Activate
    @Modified
    public void activate(CustomExternalizerConfig config) {
        this.externalHostName = config.externalHostName();
        this.externalScheme = config.externalScheme();
        this.contextPath = config.contextPath();

        log.info("Custom Externalizer initialized with hostname={}, scheme={}, contextPath={}",
                externalHostName, externalScheme, contextPath);
    }

    @Override
    public String externalize(String path) {
        if (StringUtils.isBlank(path)) {
            throw new IllegalArgumentException("Path cannot be null or empty");
        }

        if (path.startsWith(SLASH)) {
            path = path.substring(1);
        }
        if (StringUtils.isNotBlank(contextPath) && !path.startsWith(contextPath)) {
            path = contextPath + SLASH + path;
        }

        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(externalScheme)
                .append("://")
                .append(externalHostName)
                .append(SLASH)
                .append(path);

        String externalUrl = urlBuilder.toString();
        log.debug("Externalized URL for path '{}': {}", path, externalUrl);
        return externalUrl;
    }
}