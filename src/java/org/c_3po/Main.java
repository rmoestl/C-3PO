package org.c_3po;

import java.io.PrintWriter;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import org.thymeleaf.templateresolver.TemplateResolver;
import org.thymeleaf.context.*;

public class Main {
	private static TemplateEngine templateEngine;

	// Configuration Parameters
	private static String sourceDirectory = "www";
	private static String destinationDirectory = "site";
	
    public static void main(String[] args) throws Exception {
		processCmdLineArguments(args);
		System.out.println("Source directory is: " + sourceDirectory);
		System.out.println("Destination directory is: " + destinationDirectory);
		
		// TODO a c-3po.properties file on the classpath
		
		TemplateResolver templateResolver = new FileTemplateResolver();
		templateResolver.setTemplateMode("HTML5");
		templateResolver.setPrefix(sourceDirectory + "/");
		templateResolver.setSuffix(".html");
		templateEngine = new TemplateEngine();
		templateEngine.setTemplateResolver(templateResolver);
		
		Context context = new Context();
		context.setVariable("welcome", "Very well done, C-3PO!");
		
		String result = templateEngine.process("page", context);
		
        System.out.println("Hello There! I'm C-3PO! Which site do you wish me to generate?");
		
		System.out.println("----");
		System.out.println(result);		
		System.out.println("----");
		
		PrintWriter out = new PrintWriter(destinationDirectory + "/page.html");
		out.println(result);
		out.close();
    }
	
	private static void processCmdLineArguments(String[] args) {
		for (int i = 0; i < args.length; i++) {
			String argument = args[i];
			System.out.println("argument: " + argument);

			if ("-src".equals(argument) && i < args.length - 1) {
				final String sourceDirArgument = args[i + 1];
				sourceDirectory = sourceDirArgument;
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