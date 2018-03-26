package org.c_3po.cmd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Responsible for parsing and processing command line arguments.
 */
public class ArgumentsParser {
    private static final Logger LOG = LoggerFactory.getLogger(ArgumentsParser.class);

    public CmdArguments processCmdLineArguments(String[] args) {
        String sourceDirectoryName = "";
        String destinationDirectoryName = "";
        boolean autoBuild = false;

        for (int i = 0; i < args.length; i++) {
            String argument = args[i];
            LOG.debug("Command line argument: " + argument);

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

            if ("-a".equals(argument)) {
                autoBuild = true;
            }
        }

        return new CmdArguments(sourceDirectoryName, destinationDirectoryName, autoBuild);
    }
}
