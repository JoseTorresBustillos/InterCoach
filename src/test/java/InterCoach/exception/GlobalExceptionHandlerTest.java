package InterCoach.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsMissingResourcesToNotFoundResponses() {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/problems/99");

        ResponseEntity<ApiErrorResponse> response =
                handler.handleResourceNotFound(
                        new ResourceNotFoundException("Problem not found."),
                        request
                );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().path()).isEqualTo("/api/problems/99");
        assertThat(response.getBody().message()).isEqualTo("Problem not found.");
    }

    @Test
    void mapsDuplicateResourcesToConflictResponses() {
        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", "/api/users");

        ResponseEntity<ApiErrorResponse> response =
                handler.handleDuplicateResource(
                        new DuplicateResourceException("Email is already in use."),
                        request
                );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(409);
        assertThat(response.getBody().path()).isEqualTo("/api/users");
        assertThat(response.getBody().message()).isEqualTo("Email is already in use.");
    }

    @Test
    void mapsAuthenticationFailuresToUnauthorizedResponses() {
        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", "/api/auth/login");

        ResponseEntity<ApiErrorResponse> response =
                handler.handleAuthenticationFailed(
                        new AuthenticationFailedException("Invalid credentials."),
                        request
                );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(401);
        assertThat(response.getBody().path()).isEqualTo("/api/auth/login");
        assertThat(response.getBody().message()).isEqualTo("Invalid credentials.");
    }
}
