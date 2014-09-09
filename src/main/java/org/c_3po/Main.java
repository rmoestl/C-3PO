package org.c_3po;

import java.io.File;
import java.io.PrintWriter;

import org.c_3po.cmd.ArgumentsParser;
import org.c_3po.cmd.CmdArguments;
import org.c_3po.generation.SiteGenerator;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import org.thymeleaf.templateresolver.TemplateResolver;
import org.thymeleaf.context.*;

public class Main {

    public static void main(String[] args) throws Exception {

        // TODO test out Thymeleaf layout for YodaConditions
        // TODO a c-3po.properties file on the classpath

        System.out.println("Hello There! I'm C-3PO! Which site do you wish me to generate?");

        // Parsing Command Line Arguments
        CmdArguments cmdArguments = new ArgumentsParser().processCmdLineArguments(args);
        System.out.println("Source directory is: " + cmdArguments.getSourceDirectory());
        System.out.println("Destination directory is: " + cmdArguments.getDestinationDirectory());

        // Generate the Site
        SiteGenerator siteGenerator = SiteGenerator.fromCmdArguments(cmdArguments);
        siteGenerator.generate();
    }
}