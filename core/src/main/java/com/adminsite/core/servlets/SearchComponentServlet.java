package com.adminsite.core.servlets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;

@Component(service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=Search Component Servlet with Jackson",
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET,
                ServletResolverConstants.SLING_SERVLET_PATHS + "=/bin/searchComponents"
        })
public class SearchComponentServlet extends SlingSafeMethodsServlet {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String ROOT_PATH = "rootPath";
    private static final String PROPERTY_NAME = "propertyName";
    private static final String PROPERTY_VALUE = "propertyValue";
    private static final String PROPERTY_JCR_TITLE = "jcr:title";
    private static final String PROPERTY_JCR_DESCRIPTION = "jcr:description";
    private static final String PROPERTY_SLING_RES_TYPE = "sling:resourceSuperType";
    private static final String PROPERTY_VALUE_NAME = "name";
    private static final String PROPERTY_DESCRIPTION = "description";
    private static final String PROPERTY_RES_TYPE = "restype";
    private static final String PROPERTY_PATH = "path";
    private static final String APPLICATION_JSON = "application/json";

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        String rootPath = request.getParameter(ROOT_PATH);
        String propertyName = request.getParameter(PROPERTY_NAME);
        String propertyValue = request.getParameter(PROPERTY_VALUE);

        response.setContentType(APPLICATION_JSON);

        try (ResourceResolver resourceResolver = request.getResourceResolver()) {
            // Getting the root resource
            Resource rootResource = resourceResolver.getResource(rootPath);

            if (rootResource == null) {
                response.setStatus(SlingHttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write(objectMapper.createObjectNode()
                        .put("error", "Root resource not found at path: " + rootPath).toString());
                return;
            }

            ArrayNode jsonArray = objectMapper.createArrayNode();

            for (Resource subResource : rootResource.getChildren()) {
                if (matchesProperty(subResource, propertyName, propertyValue)) {
                    ObjectNode jsonObject = objectMapper.createObjectNode();
                    jsonObject.put(PROPERTY_VALUE_NAME, getProperty(subResource, PROPERTY_JCR_TITLE));
                    jsonObject.put(PROPERTY_DESCRIPTION, getProperty(subResource, PROPERTY_JCR_DESCRIPTION));
                    jsonObject.put(PROPERTY_RES_TYPE, getProperty(subResource, PROPERTY_SLING_RES_TYPE));
                    jsonObject.put(PROPERTY_PATH, subResource.getPath());
                    jsonArray.add(jsonObject);
                }
            }

            objectMapper.writeValue(response.getWriter(), jsonArray);
        } catch (Exception e) {
            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(objectMapper.createObjectNode()
                    .put("error", "Error occurred: " + e.getMessage()).toString());
        }
    }

    private String getProperty(Resource resource, String propertyName) {
        Object propertyValue = resource.getValueMap().get(propertyName);
        return propertyValue != null ? propertyValue.toString() : StringUtils.EMPTY;
    }

    private boolean matchesProperty(Resource resource, String propertyName, String propertyValue) {
        Object value = resource.getValueMap().get(propertyName);
        return value != null && propertyValue.equals(value.toString());
    }

}