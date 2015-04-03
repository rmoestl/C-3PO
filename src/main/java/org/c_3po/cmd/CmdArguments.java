package org.c_3po.cmd;

/**
 * Value class holding command line arguments.
 */
public class CmdArguments {
    private final String sourceDirectory;
    private final String destinationDirectory;
    private final boolean autoBuild;

    public CmdArguments(String sourceDirectory, String destinationDirectory, boolean autoBuild) {
        this.sourceDirectory = sourceDirectory;
        this.destinationDirectory = destinationDirectory;
        this.autoBuild = autoBuild;
    }

    public String getSourceDirectory() {
        return sourceDirectory;
    }

    public String getDestinationDirectory() {
        return destinationDirectory;
    }

    public boolean isAutoBuild() {
        return autoBuild;
    }
}
