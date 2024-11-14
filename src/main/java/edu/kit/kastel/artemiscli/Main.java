package edu.kit.kastel.artemiscli;

import edu.kit.kastel.sdq.artemis4j.ArtemisClientException;
import edu.kit.kastel.sdq.artemis4j.ArtemisNetworkException;
import edu.kit.kastel.sdq.artemis4j.client.ArtemisInstance;
import edu.kit.kastel.sdq.artemis4j.grading.ArtemisConnection;
import edu.kit.kastel.sdq.artemis4j.grading.Course;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@CommandLine.Command(
    name = "artemis",
    subcommands = {CloneCommand.class, ListLocksCommand.class, UnlockCommand.class, GradeTextAssignmentCommand.class, ShowRepositoryCommand.class},
    description = "A command line tool for interacting with the Artemis API."
)
public final class Main implements Runnable{
    @CommandLine.Spec
    CommandLine.Model.CommandSpec specification;

    @CommandLine.Option(names = {"--url"}, description = "The URL of the Artemis instance. Note that the https:// prefix is required.")
    private String artemisUrl = "https://artemis.praktomat.cs.kit.edu/";
    //private String artemisUrl = "https://artemis-test.sdq.kastel.kit.edu/";

    @CommandLine.Option(names = {"--username"}, description = "The username to use for authentication.", required = true)
    private String artemisUsername;

    // This is a char[] instead of a String, because the picocli docs recommend this for security reasons.
    @CommandLine.Option(names = {"--password"}, description = "The password to use for authentication.", interactive = true, required = true)
    private char[] artemisPassword;

    @CommandLine.Option(names = {"--course"}, description = "The id or name of the course to use. By default the latest course is used.")
    private String courseInput = null;

    private ArtemisConnection connection;
    private Course course;

    private static Course findCourse(ArtemisConnection connection, String searchTerm) throws ArtemisNetworkException {
        List<Course> courses = new ArrayList<>(connection.getCourses());

        // a newer course will have a higher id
        courses.sort(Comparator.comparingInt(Course::getId));

        if (courses.isEmpty()) {
            throw new IllegalStateException("Artemis did not return any courses");
        }

        // if no search term is provided, return the latest course
        if (searchTerm == null) {
            return courses.getLast();
        }

        List<String> availableCourses = new ArrayList<>();
        for (Course course : courses) {
            if (searchTerm.equalsIgnoreCase("" + course.getId())
                || searchTerm.equalsIgnoreCase(course.getTitle())
                || searchTerm.equalsIgnoreCase(course.getShortName())) {
                return course;
            }

            availableCourses.add("%d : '%s' : '%s'".formatted(course.getId(), course.getTitle(), course.getShortName()));
        }

        throw new IllegalStateException("Failed to find course with the search term '%s', available courses: %s".formatted(searchTerm, availableCourses));
    }

    private Main() {
    }

    private ArtemisConnection connection() {
        if (this.connection != null) {
            return this.connection;
        }

        // otherwise establish a new connection
        ArtemisInstance artemisInstance = new ArtemisInstance(this.artemisUrl);
        // TODO: support token-based authentication?
        if (this.artemisPassword == null) {
            throw new CommandLine.ParameterException(this.specification.commandLine(), "No password provided.");
        }

        try {
            this.connection = ArtemisConnection.connectWithUsernamePassword(
                artemisInstance,
                this.artemisUsername,
                new String(this.artemisPassword)
            );
        } catch (ArtemisClientException exception) {
            throw new CommandLine.ParameterException(this.specification.commandLine(), "Failed to authenticate with provided credentials: " + exception.getMessage(), exception);
        } finally {
            // Clear the password from memory after using it
            Arrays.fill(this.artemisPassword, ' ');
        }

        return this.connection;
    }

    public Course course() {
        if (this.course == null) {
            try {
                this.course = findCourse(this.connection(), this.courseInput);
            } catch (ArtemisNetworkException | IllegalStateException exception) {
                throw new CommandLine.ParameterException(this.specification.commandLine(), "Failed to find course: " + exception.getMessage(), exception);
            }
        }

        return this.course;
    }

    public void run() {
        throw new CommandLine.ParameterException(this.specification.commandLine(), "Missing required subcommand");
    }

    public static void main(String[] args) {
        Main main = new Main();
        CommandLine commandLine = new CommandLine(main);
        System.exit(commandLine.execute(args));
    }
}
