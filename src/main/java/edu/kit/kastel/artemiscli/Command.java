package edu.kit.kastel.artemiscli;

import picocli.CommandLine;

import java.util.concurrent.Callable;

/**
 * A command that can be executed from the command line.
 * <p>
 * This interface exists, because the {@link CommandLine} library requires commands
 * to implement {@link Callable} or {@link Runnable} and {@link Callable} is preferred
 * for commands that throw exceptions. The result will always be 0, so there is no point
 * in returning a value.
 */
@FunctionalInterface
interface Command extends Callable<Integer> {
    void execute() throws Exception;

    @Override
    default Integer call() throws Exception {
        this.execute();

        return 0;
    }
}
