package edu.kit.kastel.artemiscli;

import edu.kit.kastel.sdq.artemis4j.ArtemisClientException;
import edu.kit.kastel.sdq.artemis4j.grading.ClonedProgrammingSubmission;
import edu.kit.kastel.sdq.artemis4j.grading.ProgrammingExercise;
import edu.kit.kastel.sdq.artemis4j.grading.ProgrammingSubmission;
import picocli.CommandLine;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Predicate;

@CommandLine.Command(name = "clone", mixinStandardHelpOptions = true,
    description = "Clones the given submission.")
public class CloneCommand implements Command {
    @CommandLine.Spec
    private CommandLine.Model.CommandSpec specification;

    @CommandLine.Option(names = {"--student"}, description = "The username of the student.")
    private String studentId = null;

    @CommandLine.ParentCommand
    private Main parent;

    @CommandLine.Option(names = {"--exercise"}, description = "The id, name or short name of the exercise.", required = true)
    private String exerciseName;

    @CommandLine.Option(names = {"--output-folder"}, description = "The output folder.")
    // if the output folder is not set, use the current directory as the default
    private Path outputFolder = Paths.get("").toAbsolutePath();


    @CommandLine.Option(names = {"--all"}, description = "Clones all submissions.")
    private boolean shouldCloneAll;

    @Override
    public void execute() throws Exception {
        if (this.studentId == null && !this.shouldCloneAll) {
            throw new CommandLine.MissingParameterException(
                this.specification.commandLine(),
                List.of(
                    this.specification.findOption("--student"),
                    this.specification.findOption("--all")
                ),
                "Either the --student or --all option must be set."
            );
        }

        var course = this.parent.course();
        System.out.println("Course is " + course.getTitle());

        ProgrammingExercise exercise = ArtemisUtil.findExercise(course, this.exerciseName);

        Path outputFolder = Paths.get(this.outputFolder.toString(), exercise.getShortName());
        if (this.shouldCloneAll) {
            cloneSubmissions(exercise, outputFolder, a -> true);
        } else {
            cloneSubmissions(
                exercise,
                outputFolder,
                submission -> submission.getStudent().map(student -> this.studentId.equals(student.getLogin())).orElse(false)
            );
        }
    }

    private static void cloneSubmissions(ProgrammingExercise exercise, Path outputFolder, Predicate<? super ProgrammingSubmission> shouldClone) throws ArtemisClientException {
        List<ProgrammingSubmission> submissions = exercise.fetchSubmissions(0, false);
        System.out.println("Found " + submissions.size() + " submissions");

        for (ProgrammingSubmission submission : submissions) {
            if (!shouldClone.test(submission)) {
                continue;
            }

            System.out.println("Submission " + submission.getId() + " is from " + submission.getRepositoryUrl());
            Path output = Path.of(outputFolder.toString(), extractFolderName(submission.getRepositoryUrl()));

            // ignore the submission/do not close it, otherwise the folder will be deleted
            ClonedProgrammingSubmission _cloned = submission.cloneViaVCSTokenInto(output, null);
        }
    }

    private static String extractFolderName(String url) {
        String[] parts = url.split("/");
        return parts[parts.length - 1].replace(".git", "");
    }
}
