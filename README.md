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

##More Resources
If you want to dive deeper into C-3PO, have a look into the Wiki.

