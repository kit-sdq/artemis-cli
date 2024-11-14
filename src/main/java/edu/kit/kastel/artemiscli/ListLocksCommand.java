package edu.kit.kastel.artemiscli;

import edu.kit.kastel.sdq.artemis4j.ArtemisNetworkException;
import edu.kit.kastel.sdq.artemis4j.client.CourseDTO;
import edu.kit.kastel.sdq.artemis4j.client.GenericSubmissionDTO;
import edu.kit.kastel.sdq.artemis4j.grading.Course;
import edu.kit.kastel.sdq.artemis4j.grading.ProgrammingExercise;
import edu.kit.kastel.sdq.artemis4j.grading.ProgrammingSubmission;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@CommandLine.Command(name = "list-locks", mixinStandardHelpOptions = true,
    description = "Lists all currently locked submissions.")
public class ListLocksCommand implements Command {
    @CommandLine.Spec
    private CommandLine.Model.CommandSpec specification;

    @CommandLine.ParentCommand
    private Main parent;

    @CommandLine.Option(names = {"--exercise"}, description = "Will only show locks in the given exercise.", required = false)
    private String exerciseName;

    @Override
    public void execute() throws Exception {
        var exercises = ArtemisUtil.listAllApplyingExercises(this.parent.course(), this.exerciseName);

        for (ProgrammingExercise exercise : exercises) {
            for (var submission : listLockedSubmissions(this.parent.course(), exercise)) {
                System.out.printf(
                    "The submission by %s with the id %s is currently locked by %s.%n",
                    submission.getStudent().map(Object::toString).orElse("???"),
                    submission.getId(),
                    submission.getAssessor().map(Object::toString).orElse("???")
                );
            }
        }
    }

    static List<ProgrammingSubmission> listLockedSubmissions(Course course, ProgrammingExercise exercise) throws ArtemisNetworkException {
        Set<Long> allLockedSubmissions = CourseDTO.fetchLockedSubmissions(course.getConnection().getClient(), course.getId())
            .stream()
            .map(GenericSubmissionDTO::id)
            .collect(Collectors.toSet());

        if (allLockedSubmissions.isEmpty()) {
            return List.of();
        }

        if (exercise != null) {
            List<ProgrammingSubmission> allSubmissions = exercise.fetchSubmissions(0, false);
            if (exercise.hasSecondCorrectionRound()) {
                allSubmissions.addAll(exercise.fetchSubmissions(1, false));
            }


            return allSubmissions.stream()
                .filter(submission -> allLockedSubmissions.contains(submission.getId()))
                .toList();
        }

        List<ProgrammingSubmission> result = new ArrayList<>();
        Deque<ProgrammingExercise> exercises = new LinkedList<>(course.getProgrammingExercises());
        while (!allLockedSubmissions.isEmpty() && !exercises.isEmpty()) {
            ProgrammingExercise currentExercise = exercises.removeLast();
            Collection<ProgrammingSubmission> submissions = new ArrayList<>(currentExercise.fetchSubmissions(0, false));
            if (currentExercise.hasSecondCorrectionRound()) {
                submissions.addAll(currentExercise.fetchSubmissions(1, false));
            }

            for (var submission : submissions) {
                if (allLockedSubmissions.remove(submission.getId())) {
                    result.add(submission);
                }
            }
        }

        return result;
    }
}
