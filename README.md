# C-3PO

C-3PO is a static website generator for the JVM.

First and foremost C-3PO is using the [Thymeleaf 2.1.3](http://www.thymeleaf.org/doc/tutorials/2.1/usingthymeleaf.html)
templating system. Thymeleaf is a template engine (like JSP) but also
involves good layout support (like Tiles).


## Requirements

C-3PO requires a Java 11 JRE installed on you computer. Upon installation you
also need Gradle installed. C-3PO has been tested with Gradle 3.0 and 2.12. 

You'd also need to install [purifycss](https://www.npmjs.com/package/purify-css) if you'd like to purge unused CSS.


## Setup

At the moment, only installing from source is supported. Follow these steps

- ensure a Java SE JDK 11 is installed on your computer
- ensure Gradle (version 3.0, version 2.12 should be fine too) is installed on your computer
- clone C-3PO's repository to your machine
- in the repository's root directory call `gradle installDist`
- add the directory **_build/install/C-3PO/bin_** to your **PATH**


## Usage

C-3PO is a command line tool, both for Windows and Unix. C-3PO accepts these command line paramters:

- `-src <dir-name>` ... the root source directory of your website.
- `-dest <dir-name>` ... the root destination directory in which the website should be generated into.
- `-a` ... if the flag is set, C-3PO builds the website as soon as files have changed in the source directory tree. This is a useful option when fiddling around with CSS for example.
- `--fingerprint` ... if set, C-3PO fingerprints static asset files like stylesheets, JavaScript files and images (supported image file extensions are *.png*, *.jpg*, *.jpeg*, *.svg*, *.gif*, *.webp*) and replaces references to them in generated HTML documents accordingly.
- `--purge-unused-css` ... if set, attempts to purge unused CSS rules in all CSS files beneath `./css`. For this to work, [purifycss](https://www.npmjs.com/package/purify-css) needs to be installed and configured properly in `.c3posettings`.
- `-p` ... stands for production and automatically sets `--fingerprint` and `--purge-unused-css`. 

**Heads up!** C-3PO is preventing you from accidentally using the same `src` and `dest` directories because this would mean that the source files would be overwritten by their generated counterparts.

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

- *css/* --> your own CSS or SASS stylesheets
- *css/vendor* --> third-party CSS stylesheets
- *js/* --> your own JavaScript files
- *js/vendor/* --> third-party JavaScript files
- *img/* --> image files


### Samples
C-3PO comes with a sample website that would server as a good starting point for creating a new website. Take a look at `samples/base-website`.
The sample website illustrates how to:
- configure `.c3poignore` to ignore certain files
- configure `.c3posettings` to trigger convenience functions like creating a sitemap.xml file
- show how to use Thymeleaf's decorator-based layout system (Thymeleaf layout dialect)
- show how to use **markdown** to created web pages faster and in a more convenient way
- include useful stuff for web development like **CSS resets** and a **HTML5 shiv** takes makes older browsers recognize new HTML5 elements
Building the sample website is done like this: `c-3po -src samples/base-website -dest <your-dir-of-choice>`.

### Settings

C-3PO looks for a **.c3posettings** file in the top-level source directory. It's a Java standard properties file holding configuration preferences.

Here is a list of available settings:

- `baseUrl` ... the base URL of the deployed website. If not set, C-3PO does not generate a sitemap.xml file.
- `nodejsHome` ... the home directory of a nodejs binary which is required for running *purifycss* to purge unused CSS. If you're using *nvm* to manage nodejs installations, this would look something like this: `nodejsHome=/home/robert/.nvm/versions/node/v10.15.3/bin`.
- `purifycssHome` ...  the home directory of the purifycss installation which is required by C-3PO to purge unused CSS. If you're using *nvm* to manage nodejs installations, this would look something like this: `purifycssHome=/home/robert/.nvm/versions/node/v10.15.3/bin/`.
- `purifycssWhitelist` ... value supplied to purifycss' whitelist parameter which allows to whitelist CSS selectors from being purged.

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

They must start with `$meta-`. Everything between `$meta-` and the colon `:` will become the name of the meta tag. The rest after the colon `:` will be the content of the meta tag.

When processing a markdown file, C-3PO will put this data as an object called `markdownHead` into the template's context. You'll be able to use the `markdownHead` object in the Thymeleaf template file `md-template.html` like this:

```
<title th:text="${markdownHead.title}"></title>
<meta th:each="metaTagEntry : ${markdownHead.metaTags}"
  th:name="${metaTagEntry.key}"
  th:content="${metaTagEntry.value}">
```

#### Access the name of the markdown file
In some cases you'll want to access the name of the markdown file, that is being processed, in your templates (i.e. layout templates). You might know that Thymeleaf passes the name of the current template being processed to `${execInfo.templateName}`. But if a markdown file is processed, the template in use is by convention `md-template.html`, meaning that `${execInfo.templateName}` basically resolves to `md-template.html`. But for some situations you'll want to know the name of the markdown file that's being wrapped by `md-template.html`. C-3PO provides that in `markdownFileName` that you can access with `${markdownFileName}`.

**Heads up!** Before using it in your templates, you probably want to check if it is even set (e.g. when mixing markdown and html content). Here's an expression that does that: `${markdownFileName} != null`.


### Using SASS / SCSS
C-3PO is able to process **SASS / SCSS** stylesheets. SASS / SCSS is a **CSS preprocessor** and enables you to use useful things
like **selector nesting** or **variables** in your stylesheets. Read more about it at <http://sass-lang.com>.

Beware that there's a difference between `.scss` and `.sass` files. E.g. in `.sass` files you can omit curly brackets and the
indentation level of your code is important. However, seemingly the SASS owners introduced SCSS later on because
SASS was causing some confusion.

Note: C-3PO is minifying CSS output by default.

See the sample project **samples/base-website** for a basic SASS example.


### Fingerprinting assets to support cache busting

C-3PO has asset fingerprinting built-in. Right now, it supports fingerprinting CSS, JavaScript and image files. For image files, it supports the following file extensions: *.png*, *.jpg*, *.jpeg*, *.svg*, *.gif*, *.webp*.

To activate fingerprinting, either pass `--fingerprint` or `-p` on the command line. You don't have to modify your HTML (or Markdown) files for fingerprinting to work. This is in contrast to the majority of web frameworks, build tools and static site generators that require you to use special syntax to load assets. For example see the [fingerprinting section](https://guides.rubyonrails.org/asset_pipeline.html#coding-links-to-assets) in the Rails Guide or the article [How do web frameworks implement asset fingerprinting?](https://yodaconditions.net/blog/fingerprinting-assets-implementation-in-web-frameworks.html)

C-3PO fingerprinting supports all kinds of URL forms as described in [Absolute and relative URLs in HTML](https://yodaconditions.net/blog/html-url-types.html) and it is able to recognize whether an asset is served by the site under construction or by an external site. One caveat here is when referencing an asset through its absolute URL, it is only considered to be controlled by the website if the asset's base URL matches `baseUrl` in `.c3posettings`. This means, given `baseURL=https://example.com`, `https://example.com/css/main.css` is considered to be an asset of the site while the www-variant `https://www.example.com/css/main.css` is not. This would be a cool feature, but read the *solution log* for more details.

Fingerprinting by the way means that a hash of the file in question is calculated and appended to the file name. In case of C-3PO, `./css/main.css` turns into something like `./css/main.6180d1743d1be0d975ed1afbdc3b4c0bfb134124.css`. The original file is kept in place. This is a safety measure for the case something goes wrong when replacing asset references in HTML by their fingerprinted counterparts. Fingerprinting does not change the contents of the file. The hash algorithm in use is *SHA-1*. C-3PO shall produce the same hashes as the Unix command`sha1sum`.

#### Fingerprinting limitations

- There's only one way to load external CSS and JavaScript assets in HTML. For images, there are multiple ways, and so far only `<img src="...">` and `<img srcset="...">` is supported. For `srcset`, asset URLs containing a comma will not be replaced correctly. This is due to the fact, that parsing the `srcset` syntax is not trivial.
- Audio and video assets are not supported simply because this requirement didn't come up so far.
- C-3PO only fingerprints stylesheets located beneath `./css`, JavaScript files beneath `./js` and image files beneath `./img`.

### Purging unused CSS

**Heads up!** This has only been tested on Linux so far.

Purging unused CSS is the process of removing CSS rules not used on the website. Unlike with fingerprinting, C-3PO does not have this feature built-in. Instead, it relies on [purifycss](https://www.npmjs.com/package/purify-css). purifycss can be installed through npm like `npm i -g purify-css`.

purifycss has been chosen because it has the most practical CLI interface. Running such tool within the JVM's JavaScript engine (is there still one in Java 11?) was not an option.

For it to work, either supply the `--purge-unused-css` or `-p` command line arguments. In addition you need to set the `nodejsHome` and `purifycssHome` properties in `.c3posettings`. See the settings section for more information.

How does purging unused CSS relate to fingerprinting? Not much. It runs before fingerprinting and it simply replaces the original CSS file by the purified one. This means, that fingerprinting is not aware that unused CSS is purged before.

Purging unused CSS only applies to CSS files beneath the `./css` folder (including sub-directories).


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

### How to avoid obsolete &lt;div&gt; elements resulting from a decorator-based layout (layout dialect)?
Usually you do something like this in your `layout.html`

```
...
<div layout:fragment="content">
  <!-- This will be replaced by content from content template -->
</div>
...
```

... and in your content template `content1.html` something like this

```
...
<div layout:fragment="content">
  <h1>Foo Bar</h1>
</div>
...
```

But what if you want to include page-specific scripts that way? The `div` elements would surround the `script` declarations. In this case just replace `div` elements with `th:block` elements in both the `decorator` (the layout) and the `content template` (the content page).

## Development

### How to build C-3PO?

C-3PO builds with [Gradle 3.0](https://docs.gradle.org/3.0/userguide/userguide.html) and uses its [application plugin](https://docs.gradle.org/3.0/userguide/application_plugin.html).

- *gradle build* ... builds (compile, test etc.) the project
- *gradle distZip* ... creates a ZIP-packaged distribution of C-3PO
- *gradle installDist* ... installs C-3PO into *build/install*

**Hint**: you can put the **/bin** directory within the install directory to your operating system's search **PATH**. This way C-3PO will always be available on the command line.
This is very useful when developing C-3PO and building a website with C-3PO at the same time.

### Solution log

#### Decide which absolute asset URLs to consider when fingerprinting assets, 2020-04-22
When fingerprinting an asset, which hostnames are considered to be the same when it comes to replacing an absolute URL to this asset with its fingerprinted counterpart? Suppose the base URL is https://example.com. Should https://www.example.com be considered the same? For example, should https://www.example.com/css/main.css be replaced by https://www.example.com/css/main.<fingerprint>.css given the base URL (defined in `.c3posettings`) is https://example.com?

This would be a cool feature for sure. But the problem is the implementation. Unless you would also consider something like https://blog.example.com and https://www.blog.example.com the same, finding out the *lower-level domain* (the "example" in example.com, the "foo" in foo.ac.at, sometimes also called *second-level domain*) isn't easy at all. First and foremost, because top-level domains can have two levels as well, such as foo.ac.at. Hence, the common advice is to use Guava's [InternetDomainName](https://github.com/google/guava/blob/ff9fb8d30edbba5357615ecebf69120f1de556f7/android/guava/src/com/google/common/net/InternetDomainName.java) class. It uses generated regular expressions to recognize valid top-level domains. Whenever a new top-level domain gets introduced, these regular expressions need to be adapted and regenerated.

I decided not to support that feature because I didn't want to introduce a big library like Guava for such a small win. Also, it is questionable to reference one's assets through absolute URLs anyways and doing so by using either the non-www or www variant even more so. Just think about changing the domain name one time. You've got to change all those absolute URLs. Use relative URLs instead.

## More Resources
If you want to dive deeper into C-3PO, have a look into the Wiki.
