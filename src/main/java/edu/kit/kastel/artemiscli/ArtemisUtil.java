package edu.kit.kastel.artemiscli;

import edu.kit.kastel.sdq.artemis4j.ArtemisNetworkException;
import edu.kit.kastel.sdq.artemis4j.grading.Course;
import edu.kit.kastel.sdq.artemis4j.grading.ProgrammingExercise;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class ArtemisUtil {
    private ArtemisUtil() {
    }

    public static Stream<ProgrammingExercise> allProgrammingExercises(Course course) throws ArtemisNetworkException, IllegalStateException {
        return Stream.concat(course.getExams().stream().flatMap(exam -> {
            try {
                return exam.getExerciseGroups().stream();
            } catch (ArtemisNetworkException e) {
                throw new IllegalStateException(e);
            }
        }).flatMap(group -> group.getProgrammingExercises().stream()), course.getProgrammingExercises().stream());
    }

    public static ProgrammingExercise findExercise(Course course, String name) throws ArtemisNetworkException {
        if (name == null) {
            return null;
        }

        List<String> availableExercises = new ArrayList<>();

        for (var exercise : allProgrammingExercises(course).toList()) {
            if (List.of(exercise.getShortName(), exercise.getTitle(), "" + exercise.getId()).contains(name.toLowerCase())) {
                return exercise;
            }
            availableExercises.add(exercise.getShortName());
        }

        throw new IllegalStateException("Failed to find exercise with name \"%s\", available: %s".formatted(name, availableExercises));
    }


    public static List<ProgrammingExercise> listAllApplyingExercises(Course course, String exerciseName) throws ArtemisNetworkException {
        List<ProgrammingExercise> exercises = new ArrayList<>();
        if (exerciseName == null) {
            exercises.addAll(allProgrammingExercises(course).toList());
        } else {
            exercises.add(findExercise(course, exerciseName));
        }

        return exercises;
    }
}
