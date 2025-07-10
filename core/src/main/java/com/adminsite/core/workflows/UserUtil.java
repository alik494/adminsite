package com.adminsite.core.workflows;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;

import javax.jcr.RepositoryException;

import static com.day.cq.commons.jcr.JcrConstants.JCR_CREATED_BY;

@Slf4j
public class UserUtil {

    public static final String ADMINSITE_USER_SERVICE = "adminsite-user-service";
    public static final String PROFILE = "/profile";
    public static final String EMAIL = "email";

    public static String getUploaderEmail(String assetPath, ResourceResolver resolver) throws RepositoryException {
        if (resolver == null) {
            log.error("ResourceResolver is null.");
            return null;
        }
        String createdBy = getCreatedBy(assetPath, resolver);
        if (StringUtils.isBlank(createdBy)) {
            log.error("Could not determine who created the asset at path: {}", assetPath);
            return null;
        }
        return getUserEmail(createdBy, resolver);
    }

    public static String getCreatedBy(String assetPath, ResourceResolver resolver) {
        Resource resource = resolver.getResource(assetPath);
        if (resource == null) {
            log.warn("Resource does not exist at path: {}", assetPath);
            return null;
        }
        ValueMap properties = resource.getValueMap();
        return properties.get(JCR_CREATED_BY, String.class);
    }

    public static String getUserEmail(String userID, ResourceResolver resolver) throws RepositoryException {
        UserManager userManager = resolver.adaptTo(UserManager.class);
        Authorizable authorizable = userManager.getAuthorizable(userID);
        if (authorizable != null) {
            String userPath = authorizable.getPath();
            Resource profileResource = resolver.getResource(userPath + PROFILE);
            if (profileResource == null) {
                log.warn("Profile resource does not exist at path: {}", userPath);
                return null;
            }
            ValueMap properties = profileResource.getValueMap();
            String email = properties.get(EMAIL, String.class);
            if (email == null) {
                log.warn("Email is not defined for user at path: {}", userPath);
            }
            return email;
        }
        return null;
    }
}