package com.adminsite.core.servlets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
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

    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        String rootPath = request.getParameter("rootPath");
        String propertyName = request.getParameter("propertyName");
        String propertyValue = request.getParameter("propertyValue");

        response.setContentType("application/json");

        try (ResourceResolver resolver = request.getResourceResolver()) {
            Session session = resolver.adaptTo(Session.class);
            QueryManager queryManager = session.getWorkspace().getQueryManager();

            String queryString = String.format(
                    "SELECT * FROM [nt:base] AS s WHERE ISDESCENDANTNODE(s, '%s') AND s.[%s] = '%s'",
                    rootPath, propertyName, propertyValue
            );

            Query query = queryManager.createQuery(queryString, Query.JCR_SQL2);
            QueryResult result = query.execute();

            ArrayNode jsonArray = objectMapper.createArrayNode();

            NodeIterator nodes = result.getNodes();
            while (nodes.hasNext()) {
                Node resultNode = nodes.nextNode();
                ObjectNode jsonObject = objectMapper.createObjectNode();
                jsonObject.put("name", getPropertyString(resultNode, "jcr:title"));
                jsonObject.put("description", getPropertyString(resultNode, "jcr:description"));
                jsonArray.add(jsonObject);
            }

            objectMapper.writeValue(response.getWriter(), jsonArray);
        } catch (Exception e) {
            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(objectMapper.createObjectNode().put("error", e.getMessage()).toString());
        }
    }
    private String getPropertyString(Node node, String propName) throws Exception {
        if (node.hasProperty(propName)) {
            Property property = node.getProperty(propName);
            return property != null ? property.getString() : "";
        }
        return "";
    }
}