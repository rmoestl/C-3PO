package org.c_3po;

import java.io.File;
import java.io.PrintWriter;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import org.thymeleaf.templateresolver.TemplateResolver;
import org.thymeleaf.context.*;

public class Main {
	private static TemplateEngine templateEngine;

	// Configuration Parameters
	private static String sourceDirectoryName = "www";
	private static String destinationDirectory = "site";
	
    public static void main(String[] args) throws Exception {
        processCmdLineArguments(args);
        System.out.println("Hello There! I'm C-3PO! Which site do you wish me to generate?");
        System.out.println("Source directory is: " + sourceDirectoryName);
        System.out.println("Destination directory is: " + destinationDirectory);

        // TODO introduce a partials directory, partials are not copied to destination directory
        // TODO introduce an img directory, that is copied to the destination directory
        // TODO extract main functionality here into a separate class so that it can be executed within IntelliJ
        // TODO a c-3po.properties file on the classpath

        TemplateResolver templateResolver = new FileTemplateResolver();
        templateResolver.setTemplateMode("HTML5");
        templateResolver.setPrefix(sourceDirectoryName + "/");
        templateResolver.setSuffix(".html");
        templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);

        Context context = new Context();
        context.setVariable("welcome", "Very well done, C-3PO!");

        File sourceDirectory = new File(sourceDirectoryName);
        if (sourceDirectory.isDirectory()) {
            for (File file : sourceDirectory.listFiles()) {
                if (!file.isDirectory()) {
                    String result = templateEngine.process(file.getName().replace(".html", ""), context);

                    PrintWriter out = new PrintWriter(destinationDirectory + "/" + file.getName());
                    out.println(result);
                    out.close();
                }
            }
        }
    }
	
	private static void processCmdLineArguments(String[] args) {
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
				destinationDirectory = destinationDirArgument;
				i++;
			}			
		}
	}
}