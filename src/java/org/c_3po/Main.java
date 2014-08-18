package org.c_3po;

import java.io.PrintWriter;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import org.thymeleaf.templateresolver.TemplateResolver;
import org.thymeleaf.context.*;

public class Main {
	private static TemplateEngine templateEngine;
	
    public static void main(String[] args) throws Exception {
		TemplateResolver templateResolver = new FileTemplateResolver();
		templateResolver.setTemplateMode("HTML5");
		templateResolver.setPrefix("www/");
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
		
		PrintWriter out = new PrintWriter("gen/page.html");
		out.println(result);
		out.close();
    }
}