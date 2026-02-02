package com.teensconf.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class ExternalServiceException extends AppException {
    public ExternalServiceException(String message) { super(message); }
    public ExternalServiceException(String message, Throwable cause) { super(message, cause); }
}
