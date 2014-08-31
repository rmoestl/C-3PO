package org.c_3po.cmd;

/**
 * Responsible for parsing and processing command line arguments.
 */
public class ArgumentsParser {
    public CmdArguments processCmdLineArguments(String[] args) {
        String sourceDirectoryName = "";
        String destinationDirectoryName = "";

        // TODO Add checks that source directory exist

        for (int i = 0; i < args.length; i++) {
            String argument = args[i];
            System.out.println("argument: " + argument);

            if ("-src".equals(argument) && i < args.length - 1) {
                final String sourceDirArgument = args[i + 1];
                sourceDirectoryName = sourceDirArgument;
                i++;
            }

            if ("-dest".equals(argument) && i < args.length - 1) {
                final String destinationDirArgument = args[i + 1];
                destinationDirectoryName = destinationDirArgument;
                i++;
            }
        }

        return new CmdArguments(sourceDirectoryName, destinationDirectoryName);
    }
}
