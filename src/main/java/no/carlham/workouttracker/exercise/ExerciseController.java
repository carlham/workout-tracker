package no.carlham.workouttracker.exercise;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
import org.springframework.data.web.PageableDefault;

import org.springdoc.core.annotations.ParameterObject;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import no.carlham.workouttracker.security.CurrentUser;

@Tag(name = "Exercises")
@RestController
@RequestMapping("/api/exercises")
public class ExerciseController {
    private final ExerciseService service;

    public ExerciseController(ExerciseService service) {
        this.service = service;
    }

    @GetMapping
    public Page<ExerciseResponse> getAll(
            @ParameterObject @PageableDefault(size = 20, sort = "name") Pageable pageable,
            @AuthenticationPrincipal Jwt jwt) {
        return service.getAll(pageable, CurrentUser.from(jwt)).map(ExerciseResponse::from);
    }

    @PostMapping
    public ResponseEntity<ExerciseResponse> create(@Valid @RequestBody CreateExerciseRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Exercise saved = service.create(request, CurrentUser.from(jwt));
        return ResponseEntity.status(HttpStatus.CREATED).body(ExerciseResponse.from(saved));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExerciseResponse> getById(@PathVariable @Positive Long id,
            @AuthenticationPrincipal Jwt jwt) {
        return service.getById(id, CurrentUser.from(jwt))
                .map(ExerciseResponse::from)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown exercise id " + id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExerciseResponse> update(@PathVariable @Positive Long id,
            @Valid @RequestBody UpdateExerciseRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return service.update(id, request, CurrentUser.from(jwt))
                .map(exercise -> ResponseEntity.ok(ExerciseResponse.from(exercise)))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown exercise id " + id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable @Positive Long id,
            @AuthenticationPrincipal Jwt jwt) {
        service.deleteById(id, CurrentUser.from(jwt));
        return ResponseEntity.noContent().build();
    }
}
