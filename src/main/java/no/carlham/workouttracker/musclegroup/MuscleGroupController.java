package no.carlham.workouttracker.musclegroup;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/muscle-groups")
public class MuscleGroupController {
    private final MuscleGroupRepository repository;

    public MuscleGroupController(MuscleGroupRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<MuscleGroup> getAll() {

        return repository.findAll();
    }

    @PostMapping
    public ResponseEntity<MuscleGroup> create(@Valid @RequestBody CreateMuscleGroupRequest request) {
        MuscleGroup saved = repository.save(new MuscleGroup(request.name()));
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}
