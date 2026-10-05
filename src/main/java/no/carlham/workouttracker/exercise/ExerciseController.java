package no.carlham.workouttracker.exercise;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/exercises")
public class ExerciseController {
    private final ExerciseRepository repository;
    private final ExerciseService service;

    public ExerciseController(ExerciseRepository repository, ExerciseService service) {
        this.repository = repository;
        this.service = service;
    }

    @GetMapping
    public List<ExerciseResponse> getAll() {
        return repository.findAll().stream()
            .map(ExerciseResponse::from)
            .toList();
    }

    @PostMapping
    public ResponseEntity<ExerciseResponse> create(@Valid @RequestBody CreateExerciseRequest request) {
        Exercise saved = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ExerciseResponse.from(saved));
    }
}
