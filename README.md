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

Sometimes it's also desirable to exclude URLs from being crawled. This can be done in the `.c3poignore` file as described in the corresponding section.

When there is no `robots.txt` in the source folder, the C-3PO generates a minimal one putting the URL of sitemap.xml into it. This gives search crawlers a hint where to look for a sitemap file.

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

#### Ignoring files for sitemap generation only

As already mentioned in the **sitemap generation** section, C-3PO allows to ignore files and directories only in the context of sitemap generation. Simply place `es` (*exlude in sitemap*) within square brackets `[...]` after an entry of `.c3poignore`.

In the following example the directory `private` is only excluded in terms of sitemap generation:

```
.git
.idea
tasks.md
private [es]
```

#### Ignoring output of files for destination folder

Most likely you'll have a Thymeleaf layout file somewhere in the project that is an important part of the end result but that should not end up in the build directory itself. Given a `_layouts/main-layout.html` file you can exclude it from the build directory with the `[er]` modifier like this:

```
.git
.idea
tasks.md
private [es]
_layouts [er]
```

**Heads Up!** Those files and directories still trigger a build when being modified in *autoBuild* mode.


### Using Markdown
C-3PO allows you to write in markdown. To be precise [commonmark](http://commonmark.org/) is used. Why? Because it's an effort to standardize markdown syntax.

**Anyways, how do you use markdown with C-3PO?**

Create a markdown file. Then create a Thymeleaf template called `md-template.html` in the same directory. Within `md-template.html` you are able to access two `context` objects called `markdownContent` (the HTML elements that result from processing the markdown file as a `String`) and `markdownHead` (an object representing `meta` tags and `title` to be included in the page's `head`; further description below). `md-template.html` is simply a wrapper for the markdown content that allows us to integrate with the site's layout and so on.

**Heads up!** If C-3PO does not find a file called `md-template.html` in the directory, it will not process the markdown file.

**Heads up!** C-3PO does not do HTML escaping of markdown content itself since markdown allows to have inline HTML markup like `<cite>` in markdown text.

Example of a `md-template.html` file:

```
<!DOCTYPE html>
<html layout:decorator="_layouts/main-layout">
	<head>
		<title th:text="${markdownHead.title}"></title>
		<meta th:each="metaTagEntry : ${markdownHead.metaTags}"
			th:name="${metaTagEntry.key}"
			th:content="${metaTagEntry.value}">
	</head>
	<body>
		<div layout:fragment="content">
			<div th:utext="${markdownContent}">
				This is replaced by markdown based blog content.
			</div>
	</body>
</html>
```

Note the use of `th:utext` to spit out the HTML string in `markdownContent`. Beware that `th:utext` renders **unescaped** text.

#### Define HTML title and meta tags in markdown
C-3PO introduced an extension to commonmark allowing editors to define the `title` and `meta tags` for the resulting HTML page.

**Why is this useful?**

1. Reader Experience: people like when browser tabs show meaningful titles
2. SERPs: the contents of the `meta description` tag is shown on *search engine result pages (SERP)*. Ideally a description is 150 to 160 characters long. A good meta description will raise the chances that search engine users click through to your site.

**So, how do I define these meta tags?**

```
$meta-title: A catchy page title
$meta-description: A summary that describes the contents (ideally 150 to 160 characters)

# Some heading
...
```

They must start with `$meta-`. Everything between `$meta-` and the colon `:` will become the name of the meta tag. The rest after the colon `:` will be the content of the meta tag. When processing a markdown file, C-3PO will put this data as an object called `markdownHead` into the template's context. You'll be able to use the `markdownHead` object in the Thymeleaf template file `md-template.html` like this:

```
<title th:text="${markdownHead.title}"></title>
<meta th:each="metaTagEntry : ${markdownHead.metaTags}"
  th:name="${metaTagEntry.key}"
  th:content="${metaTagEntry.value}">
```



## FAQ for Website Editing

### When using Thymeleaf's Layout dialect how to pass parameters from a template to its layout?
When you use a decorator-based layout, you may want to pass parameters from
the content template to the layout template. To accomplish this simply use
`th:with` in the content template like this:

```
<html layout:decorator="_layouts/main-layout" th:with="activeMainNavEntry='home'">
```

This is useful for example when
the main navigation is defined in the layout page and you want to control
which navigation entry is rendered as active.

### How to control which navigation entry is active when using Thymeleaf's Layout dialect?
Assuming you're using Thymeleaf's [Layout dialect](https://github.com/ultraq/thymeleaf-layout-dialect)
in conjunction with decorator-based layouts there are two possibilities:

- Evaluate the standard `${execInfo.templateName}` in the layout template to determine
which template is currently being decorated. This is useful for smaller sites,
probably without a sub navigation.
- Pass a parameter, for example `activeMainNavEntry`, to the layout template and
and evaluate it when rendering the navigation. This is more useful for larger
sites where you want to control a main and a sub-navigation and the name of
the template currently being decorated doesn't reflect the main navigation category
that is supposed to be shown as active.


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
