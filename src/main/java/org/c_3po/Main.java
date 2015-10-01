package org.c_3po;

import org.c_3po.cmd.ArgumentsParser;
import org.c_3po.cmd.CmdArguments;
import org.c_3po.generation.SiteGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        Thread.currentThread().setUncaughtExceptionHandler(new UncaughtExceptionHandler());

        // TODO test out Thymeleaf layout for YodaConditions
        // TODO a c-3po.properties file on the classpath

        try {
            LOG.info("Hello There! I'm C-3PO! Which site do you wish me to generate?");

            // Parsing Command Line Arguments
            CmdArguments cmdArguments = new ArgumentsParser().processCmdLineArguments(args);
            LOG.debug("src (source directory) is: {}", cmdArguments.getSourceDirectory());
            LOG.debug("dest (destination directory) is: {}", cmdArguments.getDestinationDirectory());
            LOG.debug("autoBuild is: {}", String.valueOf(cmdArguments.isAutoBuild()));

            // Generate the Site
            SiteGenerator siteGenerator = SiteGenerator.fromCmdArguments(cmdArguments);
            if (cmdArguments.isAutoBuild()) {
                siteGenerator.generateOnFileChange();
            } else {
                siteGenerator.generate();
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