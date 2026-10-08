package no.carlham.workouttracker.exercise;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import no.carlham.workouttracker.musclegroup.MuscleGroup;
import no.carlham.workouttracker.musclegroup.MuscleGroupRepository;

@Service
public class ExerciseService {
    private final ExerciseRepository exerciseRepository;
    private final MuscleGroupRepository muscleGroupRepository;

    public ExerciseService(ExerciseRepository exerciseRepository,
        MuscleGroupRepository muscleGroupRepository
    ) {
        this.exerciseRepository = exerciseRepository;
        this.muscleGroupRepository = muscleGroupRepository;
    }

    @Transactional(readOnly = true)
    public Page<Exercise> getAll(Pageable pageable) {
        return exerciseRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Optional<Exercise> getById(Long id) {
        return exerciseRepository.findById(id);
    }

    @Transactional
    public Exercise create(CreateExerciseRequest request) {
        Exercise exercise = new Exercise(request.name(), request.comment());
        Set<Long> seenIds = new HashSet<>();

        for (var link : request.muscleGroups()) {
            if (!seenIds.add(link.muscleGroupId())) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Duplicate muscle group id " + link.muscleGroupId());
            }

            MuscleGroup musclegroup = muscleGroupRepository.findById(link.muscleGroupId())
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unknown muscle group id " + link.muscleGroupId()));

            exercise.addMuscleGroup(musclegroup, link.role());
        }

        return exerciseRepository.save(exercise);
    }

    @Transactional
    public Optional<Exercise> update(Long id, UpdateExerciseRequest request) {
        return exerciseRepository.findById(id)
            .map(exercise -> {
                exercise.updateDetails(request.name(), request.comment());
                return exerciseRepository.save(exercise);
            });
    }

    @Transactional
    public void deleteById(Long id) {
        Exercise exercise = exerciseRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Unknown exercise id " + id));

        exerciseRepository.delete(exercise);
    }

    @Transactional
    public void deleteAll() {
        exerciseRepository.deleteAll();
    }
}
