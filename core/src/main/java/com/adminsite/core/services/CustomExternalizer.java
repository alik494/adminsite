package com.adminsite.core.services;

public interface CustomExternalizer {

    /**
     * Externalizes a path to its absolute URL.
     *
     * @param path the content path to externalize
     * @return the externalized URL
     */
    String externalize(String path);
}