package nl.donniebankoebarkie.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class ApiErrorResponseFactory {

    public ApiErrorResponse create(HttpStatus status, String message, String path) {
        return new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path
        );
    }
}
