package com.adminsite.core.workflows;

import lombok.extern.slf4j.Slf4j;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

@Slf4j
public class UserUtil {
    public static final String ADMINSITE_USER_SERVICE = "adminsite-user-service";

    public static String getUserEmail(String userID, Session session) {
        try {
            String userPath = "/home/users/" + userID;
            if (session.nodeExists(userPath)) {
                Node userNode = session.getNode(userPath);
                if (userNode.hasProperty("profile/email")) {
                    return userNode.getProperty("profile/email").getString();
                } else {
                    log.warn("Email property not found for user '{}'.", userID);
                }
            } else {
                log.warn("User node does not exist at path: {}", userPath);
            }
        } catch (RepositoryException e) {
            log.error("Error retrieving user email for userID: {}", userID, e);
        } finally {
            if (session != null) {
                session.logout();
            }
        }
        return null;
    }
}