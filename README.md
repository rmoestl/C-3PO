# C-3PO

C-3PO is a Java-based static web site generator.

First and foremost C-3PO is based on the [Thymeleaf](http://www.thymeleaf.org/doc/tutorials/2.1/usingthymeleaf.html)
templating system. Thymeleaf is a template engine (like JSP) but also
involves good layout support (like Tiles).


## Requirements

C-3PO requires a Java 8 JRE installed on you computer. Upon installation you
also need Gradle 2.0 installed.


## Setup

At the moment only installing from source is supported. Follow these steps

- ensure a Java SE JDK 8 is installed on your computer
- ensure Gradle 2.0 is installed on your computer
- clone C-3PO's repository to your machine
- in the repository's root directory call `gradle installApp`
- add the directory **_build/install/C-3PO/bin_** to your **PATH**


## Usage

C-3PO is a command line tool, both for Windows and Unix. C-3PO accepts these command line paramters:

- -src <dir-name> ... the root source directory of your website
- -dest <dir-name> ... the root destination directory in which the website should be generated into
- -a ... if the flag is set, C-3PO builds the website as soon as files have changed in the source directory tree. This is a useful option when fiddling around with CSS for example.

Here's an example

```
c-3po -src . -dest site -a
```

For each file within the project directory structure C-3PO decides what to
do with it. At the moment C-3PO can

- process Thymeleaf based HTML5 files
  - Thymeleaf's [layout dialect](http://www.thymeleaf.org/doc/articles/layouts.html) is enabled
  - Thymeleaf's `LEGACYHTML5` template mode is enabled
- copy static resources like CSS and JS files into the destination directory

C-3PO does not require a certain project structure.
However, it is recommended to follow well-established standards. Here's an
example:

- *css/* --> your own CSS stylesheets
- *css/vendor* --> thired-party CSS stylesheets
- *js/* --> your own JavaScript files
- *js/vendor/* --> third-party JavaScript files
- *img/* --> image files


### Settings

C-3PO looks for a **.c3posettings** file in the top-level source directory. It's a Java standard properties file
holding configuration preferences.

Here is a list of available settings:

- baseUrl ... the base URL of the deployed website. If not set, C-3PO does not generate a sitemap.xml file.


### Generating sitemap.xml and robots.txt

C-3PO is able to generate a `sitemap.xml` (as specified at http://www.sitemaps.org) file and a `robots.txt` file.
In order to generate a sitemap.xml, C-3PO requires **two prerequisites** to be fulfilled:

- there must not exist a sitemap.xml file in the source directory (the same is true for robots.txt)
- the **baseUrl** (e.g. http://yodaconditions.net) setting must be set in `.c3posettings`

C-3PO puts the URL of the sitemap.xml file into the robots.txt file since this gives search crawlers a hint
where to look for a sitemap file.

**Heads up!** Generation of sitemap.xml and robots.txt is not supported in *autoBuild* mode.

### Ignoring certain files

You'll want to ignore certain files, e.g. the .git folder. Place a text file
named **.c3poignore** into the root directory (defined by -src). Therein list
the files and directories (one line for each) that should not be
processed by C-3PO.

#### Can I use wildcards?

Yes. The [glob](https://docs.oracle.com/javase/tutorial/essential/io/fileOps.html#glob)
syntax is supported.

#### .c3poignore example

```
.git
.idea
learnings.md
tasks.md
.editorconfig
.gitattributes
.gitignore
*.sample
```


## Development

### How to build C-3PO?

C-3PO builds with [Gradle 2.0](https://docs.gradle.org/2.0/userguide/userguide.html) and uses its [application plugin](https://docs.gradle.org/2.0/userguide/application_plugin.html).

- *gradle build* ... builds (compile, test etc.) the project
- *gradle distZip* ... creates a ZIP-packaged distribution of C-3PO
- *gradle installApp* ... installs C-3PO into *build/install*

**Hint**: you can put the **/bin** directory within the install directory to your operating system's search **PATH**. This way C-3PO will always be available on the command line.
This is very useful when developing C-3PO and building a website with C-3PO at the same time.


## More Resources
If you want to dive deeper into C-3PO, have a look into the Wiki.
