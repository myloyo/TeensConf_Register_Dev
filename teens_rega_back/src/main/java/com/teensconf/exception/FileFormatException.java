package com.teensconf.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
public class FileFormatException extends AppException {
    public FileFormatException(String message) { super(message); }
}
