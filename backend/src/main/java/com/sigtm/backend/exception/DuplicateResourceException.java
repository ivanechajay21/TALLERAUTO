package com.sigtm.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Excepción que indica que se intenta crear un recurso que ya existe.
 *
 * <p>Se traduce a una respuesta HTTP 409 (Conflict).</p>
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}