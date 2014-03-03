package org.apache.karaf4.shell.api.console;

public interface CommandLine {

    /**
     * Retrieve the argument index for the cursor position
     */
    int getCursorArgumentIndex();

    /**
     * Retrieve the argument for the cursor position
     */
    String getCursorArgument();

    /**
     * Retrieve the position of the cursor within the argument
     */
    int getArgumentPosition();

    /**
     * List of arguments on the current command.
     * If the command line contains multiple commands, only the command corresponding
     * to the cursor position is available.
     */
    String[] getArguments();

    /**
     * Retrieve the position of the cursor within the command line
     */
    int getBufferPosition();

    /**
     * Retrieve the full buffer
     */
    String getBuffer();

}
