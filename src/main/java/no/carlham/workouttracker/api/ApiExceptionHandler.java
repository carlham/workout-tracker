package no.carlham.workouttracker.api;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns every error into ProblemDetail JSON. Extending ResponseEntityExceptionHandler covers the errors
 * Spring raises itself (malformed JSON, wrong types in the URL, unsupported methods and so on).
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail handleResponseStatusException(ResponseStatusException exception) {
        return ProblemDetail.forStatusAndDetail(exception.getStatusCode(), exception.getReason());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthenticationException(AuthenticationException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Authentication required");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDeniedException(AccessDeniedException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access denied");
    }

    // An unknown ?sort= property on a paged endpoint, e.g. Swagger UI's placeholder "string"
    @ExceptionHandler(PropertyReferenceException.class)
    public ProblemDetail handlePropertyReferenceException(PropertyReferenceException exception) {
        return ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Unknown property '" + exception.getPropertyName() + "'");
    }

    // The services check for duplicates first, so this is only reached when two requests race,
    // e.g. two registrations for the same username at the same moment. The database stops the second one.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolationException(DataIntegrityViolationException exception) {
        return ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            "The request conflicts with existing data. Please try again.");
    }

    // The base class already handles the two validation exceptions, so these are overrides rather than
    // @ExceptionHandler methods (declaring both would be ambiguous). They add an "errors" map of field -> message.

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = exception.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(
                error -> error.getField(),
                error -> error.getDefaultMessage(),
                (first, second) -> first));

        return handleExceptionInternal(exception, validationProblem(errors), headers, status, request);
    }

    // Spring throws this instead of MethodArgumentNotValidException when the method also has constraints
    // on other parameters, e.g. PUT /api/exercises/{id} with @Positive on the id. Report errors the same way.
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (ParameterValidationResult result : exception.getParameterValidationResults()) {
            if (result instanceof ParameterErrors bodyErrors) {
                // A request body: report each invalid field, like the handler above
                bodyErrors.getFieldErrors()
                    .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
            } else {
                // A simple parameter such as a path variable: report it by name
                String name = result.getMethodParameter().getParameterName();
                result.getResolvableErrors()
                    .forEach(error -> errors.putIfAbsent(name, error.getDefaultMessage()));
            }
        }

        return handleExceptionInternal(exception, validationProblem(errors), headers, status, request);
    }

    private static ProblemDetail validationProblem(Map<String, String> errors) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
        problem.setProperty("errors", errors);
        return problem;
    }
}
