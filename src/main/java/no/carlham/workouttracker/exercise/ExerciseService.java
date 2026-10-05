package no.carlham.workouttracker.exercise;

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

    @Transactional
    public Exercise create(CreateExerciseRequest request) {
        Exercise exercise = new Exercise(request.name(), request.comment());

        for (var link : request.muscleGroups()) {
            MuscleGroup musclegroup = muscleGroupRepository.findById(link.muscleGroupId())
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unknown muscle group id " + link.muscleGroupId()));

            exercise.addMuscleGroup(musclegroup, link.role());
        }

        return exerciseRepository.save(exercise);
    }
}
