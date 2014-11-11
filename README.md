#C-3PO
=====

C-3PO is a JVM-based static web site generator

First and foremost C-3PO  is based on the Thymeleaf templating system. Thymeleaf is a template engine(JSP) but also involves good layout support (like Tiles).

For website editors, C-3PO comes as a command line tool, both for Windows and Unix. C-3PO accepts certain command line paramters, some of them mandatory:

- -src <dir-name> ... the root source directory of your website
- -dest <dir-name> ... the root destination directory in which the website should be generated into
- -a ... if the flag is set, C-3PO builds the website as soon as files have changed in the source directory tree. This is a nice option when doing work on CSS for example.

C-3PO emphasizes the idea of **convention over configuration** which means that there are a couple of conventions that makes it easy to use C-3PO:

- css --> here your CSS stylesheets are placed
- js --> here your JavaScript files are placed
- img --> here your image files (and possibly other media files) are placed

##More Resources
If you wanna dive deeper into C-3PO have a look into the Wiki

