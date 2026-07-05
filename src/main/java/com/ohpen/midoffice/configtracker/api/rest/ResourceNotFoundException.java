package com.ohpen.midoffice.configtracker.api.rest;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
