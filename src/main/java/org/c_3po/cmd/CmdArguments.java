package org.c_3po.cmd;

/**
 * Value class holding command line arguments.
 */
public class CmdArguments {
    private final String sourceDirectory;
    private final String destinationDirectory;

    public CmdArguments(String sourceDirectory, String destinationDirectory) {
        this.sourceDirectory = sourceDirectory;
        this.destinationDirectory = destinationDirectory;
    }

    public String getSourceDirectory() {
        return sourceDirectory;
    }

    public String getDestinationDirectory() {
        return destinationDirectory;
    }
}
