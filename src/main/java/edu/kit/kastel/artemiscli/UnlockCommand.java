package edu.kit.kastel.artemiscli;

import edu.kit.kastel.sdq.artemis4j.ArtemisNetworkException;
import edu.kit.kastel.sdq.artemis4j.client.ProgrammingSubmissionDTO;
import edu.kit.kastel.sdq.artemis4j.grading.ProgrammingSubmission;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@CommandLine.Command(name = "unlock", mixinStandardHelpOptions = true,
    description = "Unlocks the given submission(s).")
public class UnlockCommand implements Command {
    @CommandLine.Spec
    private CommandLine.Model.CommandSpec specification;

    @CommandLine.Parameters(index = "0", arity = "0..*", description = "The id(s) of the submission(s) to unlock.")
    private long[] submissionIds;

    @CommandLine.Option(names = {"--all"}, description = "Unlocks all submissions.")
    private boolean shouldUnlockAll;

    @CommandLine.ParentCommand
    private Main parent;

    @CommandLine.Option(names = {"--exercise"}, description = "The id, name or short name of the exercise.", required = false)
    private String exerciseName;

    @Override
    public void execute() throws Exception {
        if (this.submissionIds == null) {
            this.submissionIds = new long[0];
        }

        if (this.submissionIds.length == 0 && !this.shouldUnlockAll) {
            System.out.println("Warning: No submission ids provided. Use --all to unlock all submissions.");
            return;
        }

        var exercises = ArtemisUtil.listAllApplyingExercises(this.parent.course(), this.exerciseName);

        for (var exercise : exercises) {
            List<ProgrammingSubmission> availableSubmissions = ListLocksCommand.listLockedSubmissions(this.parent.course(), exercise);

            Collection<ProgrammingSubmission> submissions = new ArrayList<>();
            outer: for (long submissionId : this.submissionIds) {
                for (var submission : availableSubmissions) {
                    if (submission.getId() == submissionId) {
                        submissions.add(submission);
                        continue outer;
                    }
                }

                System.out.println("Warning: Submission with id " + submissionId + " is not locked, will be ignored.");
            }

            if (this.shouldUnlockAll) {
                submissions.addAll(availableSubmissions);
            }

            for (var submission : submissions) {
                unlock(submission);
                System.out.println("Unlocked submission " + submission.getId());
            }
        }
    }

    private static void unlock(ProgrammingSubmission submission) throws ArtemisNetworkException {
        if (submission.isSubmitted()) {
            throw new IllegalStateException("Submission has already been submitted");
        }

        ProgrammingSubmissionDTO.cancelAssessment(submission.getConnection().getClient(), submission.getId());
    }
}
