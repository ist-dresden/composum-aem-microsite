# Composum AEM - Microsite module

A page implementation to upload and embed a simple site as AEM content page.

- [Releases](https://github.com/ist-dresden/composum-aem-microsite/releases)
- [Documentation site for Composum Dashboard](https://ist-dresden.github.io/composum-aem-microsite/)

### What is the use case?

Sometimes you may want to embed a simple game, tool or similar content into your AEM site.
Perhaps you have such a simple standalone solution from an agency as a collection of files with an 'index.html'
as the starting point of the application.
With a 'Microsite Page' provided by this module, you can upload such an application as a ZIP file
into the page and use it as an AEM page.

But you guessed it, there are limitations of course. It is a simple solution that extracts the ZIP content
as page content and provides a servlet that delivers the extracted content in the context of the AEM page.
To make this possible, the links in the application code are converted to links in the context of the
AEM page context.

Therefore, your application to be embedded must be as simple as possible and use relative links
without complex bootstrap or data loading code that computes resource links. If you still need such
bootstrap code, you can use some simple patterns in the code to support this microsite solution:

- Each relative path to a resource in your JS code should start with a '```./```' path prefix.

  Any such prefix will be replaced with the AEM page path when the microsite content is extracted.

- If JS variables are used for the root path, you can embed the placeholders
  - '```${page}```' - the relative path of the AEM page as the relative root of the application in the AEM repository.
  - '```${path}```' - the absolute path of the AEM page as the absolute root of the application in the AEM repository.

- It is possible and a safer option to use data attributes for application resource paths in the HTML markup,
  attributes with the attribute names
  - '```data-path[-something]```' or '```data-file[-something]```' or '```data-resource[-something]```'

  are interpreted as relative paths to be converted, and these are then changed during extraction.

## Try it!

For testing, you can use a framework or a style sheet that you can download for free, for example:

- https://startbootstrap.com/theme/agency
- https://startbootstrap.com/theme/sb-admin-2
- https://html5up.net/

Create a 'Microsite Page' for testing and upload such an example by opening the 'Microsite' tab
in the page properties and upload it in the upload field of this tab, save the properties and that's it.
The embedded page should show up as expected on the AEM publishers and
in preview mode 'View as Published' on the author.
