package edu.kit.kastel.artemiscli;

import picocli.CommandLine;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

@CommandLine.Command(name = "show", mixinStandardHelpOptions = true,
    description = "Shows the repository urls of the given submissions.")
public class ShowRepositoryCommand implements Command {
    private static final String REPOSITORY_URL = "https://artemis.praktomat.cs.kit.edu/courses/%d/exercises/%d/repository/%d";

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec specification;

    @CommandLine.ParentCommand
    private Main parent;

    @CommandLine.Option(names = {"--exercise"}, description = "The id, name or short name of the exercise.")
    private String exerciseName;

    @CommandLine.Parameters(index = "0", arity = "1..*", description = "The submission ids or usernames of the submissions to show the repo for.")
    private String[] submissionIds;

    @Override
    public void execute() throws Exception {
        if (this.submissionIds == null) {
            System.out.println("Warning: No submission ids provided.");
            return;
        }

        Collection<String> remainingIds = new LinkedHashSet<>(List.of(this.submissionIds));
        outer: for (var exercise : ArtemisUtil.listAllApplyingExercises(this.parent.course(), this.exerciseName)) {
            for (var submission : exercise.fetchSubmissions(0, false)) {
                if (remainingIds.isEmpty()) {
                    break outer;
                }

                if (remainingIds.remove("" + submission.getId()) || submission.getStudent().isPresent() && remainingIds.contains(submission.getStudent().get().toString())) {
                    String prefix = "[%s][%s][%s]:".formatted(exercise.getTitle(), submission.getId(), submission.getStudent().get().toString());
                    System.out.printf("%s git url to clone: %s%n".formatted(prefix, submission.getRepositoryUrl()));
                    System.out.printf("%s artemis url:      %s%n".formatted(" ".repeat(prefix.length()), REPOSITORY_URL.formatted(this.parent.course().getId(), exercise.getId(), submission.getParticipationId())));
                }
            }
        }


        if (remainingIds.stream().anyMatch(id -> id.matches("\\d+"))) {
            System.out.println("Warning: The following submissions could not be found:");
            for (var id : remainingIds) {
                System.out.println(id);
            }
        }
    }
}
