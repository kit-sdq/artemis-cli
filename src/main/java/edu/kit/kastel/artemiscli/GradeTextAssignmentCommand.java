package edu.kit.kastel.artemiscli;

import edu.kit.kastel.sdq.artemis4j.ArtemisNetworkException;
import edu.kit.kastel.sdq.artemis4j.grading.Course;
import edu.kit.kastel.sdq.artemis4j.grading.TextExercise;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

@CommandLine.Command(name = "grade-text", mixinStandardHelpOptions = true,
    description = "Grades a text assignment.")
public class GradeTextAssignmentCommand implements Command {
    private static final int NUMBER_OF_TRIES = 800;

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec specification;

    @CommandLine.ParentCommand
    private Main parent;

    @CommandLine.Option(names = {"--exercise"}, description = "Will grade the given exercise.", required = true)
    private String exerciseName;

    @CommandLine.Option(names = {"--points"}, description = "The points to give for a valid submission.", required = true)
    private double points;

    @CommandLine.Parameters(index = "0", arity = "0..*", description = "Regular expressions that define valid submissions. Note that invalid submissions will be ignored.")
    private String[] validRegexes;


    private boolean isValidSubmission(String text) {
        if (text == null) {
            return false;
        }

        return Arrays.stream(this.validRegexes).anyMatch(regex -> text.trim().matches(regex));
    }

    @Override
    public void execute() throws Exception {
        TextExercise exercise = findExercise(this.parent.course(), this.exerciseName);

        if (this.validRegexes == null) {
            System.out.println("Warning: No regexes for valid submissions provided, all submissions will be ignored.");
            return;
        }

        Collection<String> seenSubmissions = new HashSet<>();
        int i = 0;
        // the code doesn't use the listSubmissions endpoint, because the locking from there is not allowed for tutors
        // for text exercises specifically (for programming submissions that works)
        var assessment = exercise.tryLockNextSubmission(0).orElse(null);
        while (assessment != null && i < NUMBER_OF_TRIES) {
            String text = assessment.getText().trim();

            boolean isNewSubmission = seenSubmissions.add(text);
            boolean isValid = isValidSubmission(text);
            i++;

            if (isValid) {
                assessment.addAnnotation(this.points, "Super gemacht :)", 0, text.length());
                assessment.submit();
                assessment = exercise.tryLockNextSubmission(0).orElse(null);
                continue;
            }

            if (isNewSubmission) {
                System.out.println("The submission is invalid: '%s'".formatted(text));
            }

            assessment.cancel();
            assessment = exercise.tryLockNextSubmission(0).orElse(null);
        }
    }

    private static TextExercise findExercise(Course course, String name) throws ArtemisNetworkException {
        if (name == null) {
            return null;
        }

        List<String> availableExercises = new ArrayList<>();

        for (var exercise : course.getTextExercises()) {
            if (name.equalsIgnoreCase(exercise.getShortName()) || name.equalsIgnoreCase(exercise.getTitle())) {
                return exercise;
            }
            availableExercises.add(exercise.getShortName());
        }

        throw new IllegalStateException("Failed to find exercise with name \"%s\", available: %s".formatted(name, availableExercises));
    }
}
