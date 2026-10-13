package no.carlham.workouttracker.session;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import org.springdoc.core.annotations.ParameterObject;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import no.carlham.workouttracker.security.CurrentUser;
import no.carlham.workouttracker.session.SessionResponse.SetResponse;

@Tag(name = "Sessions")
@RestController
@RequestMapping("/api/sessions")
public class SessionController {
    private final SessionService service;

    public SessionController(SessionService service) {
        this.service = service;
    }

    @GetMapping
    public Page<SessionResponse> getMine(
            @ParameterObject @PageableDefault(size = 20, sort = "performedAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal Jwt jwt) {
        return service.getMine(pageable, CurrentUser.from(jwt)).map(SessionResponse::from);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SessionResponse> getById(@PathVariable @Positive Long id,
            @AuthenticationPrincipal Jwt jwt) {
        return service.getById(id, CurrentUser.from(jwt))
                .map(SessionResponse::from)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown session id " + id));
    }

    @PostMapping
    public ResponseEntity<SessionResponse> create(@Valid @RequestBody CreateSessionRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        WorkoutSession saved = service.create(request, CurrentUser.from(jwt));
        return ResponseEntity.status(HttpStatus.CREATED).body(SessionResponse.from(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SessionResponse> update(@PathVariable @Positive Long id,
            @Valid @RequestBody UpdateSessionRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return service.update(id, request, CurrentUser.from(jwt))
                .map(session -> ResponseEntity.ok(SessionResponse.from(session)))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown session id " + id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable @Positive Long id,
            @AuthenticationPrincipal Jwt jwt) {
        service.deleteById(id, CurrentUser.from(jwt));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/sets")
    public ResponseEntity<SetResponse> addSet(@PathVariable @Positive Long id,
            @Valid @RequestBody SetRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        SessionSet set = service.addSet(id, request, CurrentUser.from(jwt));
        return ResponseEntity.status(HttpStatus.CREATED).body(SetResponse.from(set));
    }

    @PutMapping("/{id}/sets/{setId}")
    public SetResponse updateSet(@PathVariable @Positive Long id, @PathVariable @Positive Long setId,
            @Valid @RequestBody UpdateSetRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return SetResponse.from(service.updateSet(id, setId, request, CurrentUser.from(jwt)));
    }

    @DeleteMapping("/{id}/sets/{setId}")
    public ResponseEntity<Void> deleteSet(@PathVariable @Positive Long id, @PathVariable @Positive Long setId,
            @AuthenticationPrincipal Jwt jwt) {
        service.deleteSet(id, setId, CurrentUser.from(jwt));
        return ResponseEntity.noContent().build();
    }
}
