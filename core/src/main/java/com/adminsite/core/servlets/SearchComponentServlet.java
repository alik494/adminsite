package com.adminsite.core.servlets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;

import javax.jcr.query.Query;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Iterator;

import static com.day.cq.commons.jcr.JcrConstants.JCR_DESCRIPTION;
import static com.day.cq.commons.jcr.JcrConstants.JCR_TITLE;
import static com.day.cq.wcm.foundation.forms.FormsConstants.PROPERTY_RST;
import static org.apache.oltu.oauth2.common.OAuth.ContentType.JSON;

@Slf4j
@Component(service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=Search Component Servlet Using findResources",
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET,
                ServletResolverConstants.SLING_SERVLET_PATHS + "=/bin/searchComponents"
        })
public class SearchComponentServlet extends SlingSafeMethodsServlet {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String ROOT_PATH = "rootPath";
    private static final String PROPERTY_NAME = "propertyName";
    private static final String PROPERTY_VALUE = "propertyValue";
    private static final String PROPERTY_VALUE_NAME = "name";
    private static final String PROPERTY_DESCRIPTION = "description";
    private static final String PROPERTY_RES_TYPE = "restype";
    private static final String PROPERTY_PATH = "path";

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        String rootPath = request.getParameter(ROOT_PATH);
        String propertyName = request.getParameter(PROPERTY_NAME);
        String propertyValue = request.getParameter(PROPERTY_VALUE);

        response.setContentType(JSON);

        if (StringUtils.isBlank(rootPath)) {
            log.error("Missing required parameter: {}", ROOT_PATH);
            response.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(objectMapper.createObjectNode()
                    .put("error", "Missing required parameter: " + ROOT_PATH).toString());
            return;
        }

        try (ResourceResolver resourceResolver = request.getResourceResolver()) {

            StringBuilder queryBuilder = new StringBuilder("SELECT * FROM [nt:base] AS node WHERE ISDESCENDANTNODE(node, '")
                    .append(rootPath).append("')");

            if (StringUtils.isNotBlank(propertyName) && StringUtils.isNotBlank(propertyValue)) {
                queryBuilder.append(" AND node.[").append(propertyName).append("] = '").append(propertyValue).append("'");
            }

            String query = queryBuilder.toString();
            log.debug("Executing query: {}", query);

            Iterator<Resource> results =  resourceResolver.findResources(query, Query.JCR_SQL2);
            ArrayNode jsonArray = objectMapper.createArrayNode();

            for (Iterator<Resource> it = results; it.hasNext(); ) {
                Resource resource = it.next();
                log.debug("Matched resource: {}", resource.getPath());
                ObjectNode jsonObject = objectMapper.createObjectNode();
                jsonObject.put(PROPERTY_VALUE_NAME, getProperty(resource, JCR_TITLE));
                jsonObject.put(PROPERTY_DESCRIPTION, getProperty(resource, JCR_DESCRIPTION));
                jsonObject.put(PROPERTY_RES_TYPE, getProperty(resource, PROPERTY_RST));
                jsonObject.put(PROPERTY_PATH, resource.getPath());
                jsonArray.add(jsonObject);
            }

            objectMapper.writeValue(response.getWriter(), jsonArray);
        } catch (Exception e) {
            log.error("An error occurred while processing the request", e);
            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(objectMapper.createObjectNode()
                    .put("error", "Error occurred: " + e.getMessage()).toString());
        }
    }

    private String getProperty(Resource resource, String propertyName) {
        Object propertyValue = resource.getValueMap().get(propertyName);
        if (propertyValue != null) {
            log.debug("Property found: {} = {}", propertyName, propertyValue.toString());
        }
        return propertyValue != null ? propertyValue.toString() : StringUtils.EMPTY;
    }
}