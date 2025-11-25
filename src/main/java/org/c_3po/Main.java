package org.c_3po;

import org.c_3po.cmd.CmdArguments;
import org.c_3po.editing.EditMode;
import org.c_3po.generation.NewDraft;
import org.c_3po.generation.SiteGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        Thread.currentThread().setUncaughtExceptionHandler(new UncaughtExceptionHandler());

        // TODO test out Thymeleaf layout for YodaConditions
        // TODO a c-3po.properties file on the classpath

        try {
            LOG.info("Hello There! I'm C-3PO! Which site do you wish me to generate?");

            // Parsing command line arguments
            final CmdArguments cmdArgs = CmdArguments.parse(args);
            cmdArgs.logArguments();

            // Validate command line arguments
            final boolean cmdArgsValid = cmdArgs.validate();

            // Execute the requested C-3PO command
            if (cmdArgsValid) {
                SiteGenerator siteGenerator = SiteGenerator.fromCmdArguments(cmdArgs);
                if (cmdArgs.isAutoBuild()) {
                    siteGenerator.generateOnFileChange();
                } else if (cmdArgs.isNewDraftModeEnabled()) {
                    Path draftFilePath = NewDraft.getFilePathFrom(cmdArgs);
                    EditMode.start(draftFilePath, siteGenerator);
                } else if (cmdArgs.isEditModeEnabled()) {
                    Path filePath = EditMode.getFileToEditFrom(cmdArgs);
                    EditMode.start(filePath, siteGenerator);
                } else {
                    siteGenerator.generate();
                }
            }

            LOG.debug("I'm going to shutdown.");
        } catch (RuntimeException ex) {
            LOG.error("Caught a runtime exception in main method. Terminating with a non-zero exit code", ex);
            System.exit(1);
        }
    }

    private static class UncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            LOG.error("Caught an uncaught exception.", e);
        }
    }
}