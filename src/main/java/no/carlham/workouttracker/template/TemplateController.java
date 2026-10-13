package no.carlham.workouttracker.template;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

@Tag(name = "Templates")
@RestController
@RequestMapping("/api/templates")
public class TemplateController {
    private final TemplateService service;

    public TemplateController(TemplateService service) {
        this.service = service;
    }

    @GetMapping
    public Page<TemplateResponse> getMine(
            @ParameterObject @PageableDefault(size = 20, sort = "name") Pageable pageable,
            @AuthenticationPrincipal Jwt jwt) {
        return service.getMine(pageable, CurrentUser.from(jwt)).map(TemplateResponse::from);
    }

    @GetMapping("/public")
    public Page<TemplateResponse> getPublic(
            @ParameterObject @PageableDefault(size = 20, sort = "name") Pageable pageable,
            @AuthenticationPrincipal Jwt jwt) {
        return service.getPublic(pageable, CurrentUser.from(jwt)).map(TemplateResponse::from);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TemplateResponse> getById(@PathVariable @Positive Long id,
            @AuthenticationPrincipal Jwt jwt) {
        return service.getById(id, CurrentUser.from(jwt))
                .map(TemplateResponse::from)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown template id " + id));
    }

    @PostMapping
    public ResponseEntity<TemplateResponse> create(@Valid @RequestBody TemplateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        WorkoutTemplate saved = service.create(request, CurrentUser.from(jwt));
        return ResponseEntity.status(HttpStatus.CREATED).body(TemplateResponse.from(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TemplateResponse> update(@PathVariable @Positive Long id,
            @Valid @RequestBody TemplateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return service.update(id, request, CurrentUser.from(jwt))
                .map(template -> ResponseEntity.ok(TemplateResponse.from(template)))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown template id " + id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable @Positive Long id,
            @AuthenticationPrincipal Jwt jwt) {
        service.deleteById(id, CurrentUser.from(jwt));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/copy")
    public ResponseEntity<TemplateResponse> copy(@PathVariable @Positive Long id,
            @AuthenticationPrincipal Jwt jwt) {
        WorkoutTemplate copy = service.copy(id, CurrentUser.from(jwt));
        return ResponseEntity.status(HttpStatus.CREATED).body(TemplateResponse.from(copy));
    }
}
