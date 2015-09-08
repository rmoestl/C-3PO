#C-3PO

C-3PO is a Java-based static web site generator.

First and foremost C-3PO is based on the Thymeleaf templating system. Thymeleaf is a template engine (like JSP) but also involves good layout support (like Tiles).

C-3PO is a command line tool, both for Windows and Unix. C-3PO accepts these command line paramters:

- -src <dir-name> ... the root source directory of your website
- -dest <dir-name> ... the root destination directory in which the website should be generated into
- -a ... if the flag is set, C-3PO builds the website as soon as files have changed in the source directory tree. This is a useful option when fiddling around with CSS for example.

C-3PO emphasizes the idea of **convention over configuration** which means that there are a couple of conventions that make it easy to use C-3PO:

- *css* --> CSS stylesheets directory
- *js* --> JavaScript files directory
- *img* --> image files (and possibly other media files) directory

##Development

###How to build C-3PO?

C-3PO builds with [Gradle 2.0](https://docs.gradle.org/2.0/userguide/userguide.html) and uses it's [application plugin](https://docs.gradle.org/2.0/userguide/application_plugin.html).

- *gradle build* ... builds (compile, test etc.) the project
- *gradle distZip* ... creates a ZIP-packaged distribution of C-3PO
- *gradle installApp* ... installs C-3PO into *build/install*

**Hint**: you can put the **/bin** directory within the install directory to your operating system's search **PATH**. This way C-3PO will always be available on the command line. 
This is very useful when developing C-3PO and building a website with C-3PO at the same time.

##More Resources
If you want to dive deeper into C-3PO, have a look into the Wiki.

