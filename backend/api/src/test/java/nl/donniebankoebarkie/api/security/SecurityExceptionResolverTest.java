package nl.donniebankoebarkie.api.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.servlet.HandlerExceptionResolver;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SecurityExceptionResolverTest {

    private final HandlerExceptionResolver handlerExceptionResolver = mock(HandlerExceptionResolver.class);
    private final SecurityExceptionResolver securityExceptionResolver =
            new SecurityExceptionResolver(handlerExceptionResolver);
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);

    @Test
    void commenceDelegatesAuthenticationExceptionsToMvcExceptionResolver() throws Exception {
        AuthenticationException exception = mock(AuthenticationException.class);

        securityExceptionResolver.commence(request, response, exception);

        verify(handlerExceptionResolver).resolveException(request, response, null, exception);
    }

    @Test
    void handleDelegatesAccessDeniedExceptionsToMvcExceptionResolver() throws Exception {
        AccessDeniedException exception = new AccessDeniedException("Forbidden");

        securityExceptionResolver.handle(request, response, exception);

        verify(handlerExceptionResolver).resolveException(request, response, null, exception);
    }
}
