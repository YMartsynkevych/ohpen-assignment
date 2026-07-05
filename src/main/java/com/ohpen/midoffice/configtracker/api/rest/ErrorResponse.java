package com.ohpen.midoffice.configtracker.api.rest;

import java.time.LocalDateTime;

public record ErrorResponse(
    String errorCode,
    String message,
    LocalDateTime timestamp
) {}
