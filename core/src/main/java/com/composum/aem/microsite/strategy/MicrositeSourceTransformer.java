package com.composum.aem.microsite.strategy;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.composum.aem.microsite.MicrositeConstants.PLACEHOLDER_PAGE;
import static com.composum.aem.microsite.MicrositeConstants.PLACEHOLDER_PATH;

/**
 * this transformer is rewriting relative URLs in the source files of an imported ZIP site
 */
public class MicrositeSourceTransformer {

    /**
     * url transformation patterns
     */
    // HTML: <a|link ... href="uri/to/target.ext" ... >
    public static final Pattern PTN_HTML_HREF = Pattern
            .compile("(?<start><\\s*(a|link)\\s+([^>]+\\s+)?href\\s*=\\s*[\"'])(?<change>[^\"'#]+)(?<end>[\"'][^>]*>)");
    // HTML: <img|audio|video|source|script|frame ... src="uri/to/file.ext" ... >
    public static final Pattern PTN_HTML_SRC = Pattern
            .compile("(?<start><\\s*(img|audio|video|source|script|frame)\\s+([^>]+\\s+)?src\\s*=\\s*[\"'])(?<change>[^\"']+)(?<end>[\"'][^>]*>)");
    // HTML: <div ... data-file="path/to/file.ext" ... >
    public static final Pattern PTN_HTML_DATA = Pattern
            .compile("(?<start><\\s*(div)\\s+([^>]+\\s+)?data-(path|file|resource)(-[a-zA-Z0-9_-])?\\s*=\\s*[\"'])(?<change>[^\"']+)(?<end>[\"'][^>]*>)");
    // JS,JSON: "some/${path}/to/file.ext" or './to/file.ext'
    public static final Pattern PTN_PATH_VAR = Pattern
            .compile("(?<start>[\"'])(?<change>([^$\"']*\\$\\{(" + PLACEHOLDER_PAGE + "|" + PLACEHOLDER_PATH + ")}|\\./)[^\"']*)(?<end>[\"'])");

    /**
     * decide between external or absolute and relative URLs
     */
    public static final Pattern PTN_EXT_ABS_URL = Pattern
            .compile("^((?<scheme>https?):)?//((?<host>[^:/]+)(:(?<port>\\d+))?)?(?<path>/.*)$");

    /**
     * the base URL for all relative URIs in the source
     */
    protected final String relativeRoot; // the relative root starts with the proxy page itself

    /**
     * the base URL for all absolure URIs in the source
     */
    protected final String absoluteRoot;

    /**
     * constructor with the URL (path) of the landing page as general base for all sources
     *
     * @param relativeRoot the base URL for all relative URLs to HTML targets in the source
     */
    public MicrositeSourceTransformer(@NotNull final String absoluteRoot, @NotNull final String relativeRoot) {
        this.absoluteRoot = absoluteRoot;
        this.relativeRoot = relativeRoot;
    }

    /**
     * build the URL relative to the microsite root relative to url base of the transformed source
     *
     * @param relBase the base path of the source relative to the source root
     * @param url     the URL from the original source to transform
     * @param isIndex 'true' if transforming the 'index.html' file
     * @return the transformed URL in relation to the root base
     */
    public String transformUrl(String relBase, String url, boolean isIndex, String indexPath, boolean htmlContext) {
        StringBuilder transformedUrl = null;
        Matcher external = PTN_EXT_ABS_URL.matcher(url);
        if (!external.matches()) { // ignore external URLs
            String targetExt = StringUtils.substringAfterLast(url, ".");
            final String relPath = relativeRoot + relBase;
            if (url.contains("${" + PLACEHOLDER_PAGE + "}")) {
                transformedUrl = new StringBuilder(url.replaceFirst("\\$\\{" + PLACEHOLDER_PAGE + "}", relPath));
            } else if (url.contains("${" + PLACEHOLDER_PATH + "}")) {
                transformedUrl = new StringBuilder(url.replaceFirst("\\$\\{" + PLACEHOLDER_PATH + "}", absoluteRoot));
            } else if (url.startsWith("./")) {
                transformedUrl = new StringBuilder(relPath + url.substring(1));
            } else {
                transformedUrl = new StringBuilder(relPath + "/" + url);
            }
            transformedUrl = new StringBuilder(transformedUrl.toString().replaceAll("/\\./", "/")); // remove '/.' path segments
            transformedUrl = new StringBuilder(transformedUrl.toString().replaceAll("/[^/]+/\\.\\./", "/"));
            if (htmlContext) {
                if (transformedUrl.toString().equals(relativeRoot + (indexPath.startsWith("/") ? indexPath : "/" + indexPath))) {
                    transformedUrl = new StringBuilder(relativeRoot);
                }
                if (!isIndex) {
                    // let's start with the folder of the landing page if a content file - not the index - references something
                    transformedUrl.insert(0, StringUtils.repeat("../", StringUtils.countMatches(relativeRoot + relBase, "/") + 1));
                }
            }
        }
        return transformedUrl != null ? transformedUrl.toString() : url;
    }

    /**
     * transforms an HTML source file and rewrites all URLs to use the landing page as base URL
     *
     * @param relBase the base path of the transformed file relative to the root URL
     * @param source  the source code to transform
     * @param isIndex 'true' if transforming the 'index.html' file
     * @return the transformed source code
     */
    public String transformHtml(String relBase, String source, boolean isIndex, String indexPath) {
        String result = source;
        result = transform(relBase, result, isIndex, indexPath, true, PTN_HTML_HREF);
        result = transform(relBase, result, isIndex, indexPath, true, PTN_HTML_SRC);
        result = transform(relBase, result, isIndex, indexPath, true, PTN_HTML_DATA);
        result = transform(relBase, result, isIndex, indexPath, true, PTN_PATH_VAR);
        return result;
    }

    /**
     * transforms the source string applying one pattern
     *
     * @param relBase the base path of the transformed file relative to the root URL
     * @param source  the source code to transform
     * @param isIndex 'true' if transforming the 'index.html' file
     * @param pattern the pattern to apply
     * @return the transformed source code
     */
    public String transform(String relBase, String source,
                            boolean isIndex, String indexPath, boolean htmlContext,
                            Pattern pattern) {
        StringBuilder transformed = new StringBuilder();
        int len = source.length();
        int pos = 0;
        Matcher matcher = pattern.matcher(source);
        while (matcher.find(pos)) {
            String url = matcher.group("change"); // extract URL from pattern
            url = transformUrl(relBase, url, isIndex, indexPath, htmlContext); // and transform the URL
            transformed.append(source, pos, matcher.start()); // copy up to the start of the pattern
            transformed.append(matcher.group("start")); // copy unmodified start sequence of the pattern
            transformed.append(url); // add transformed URL part of the pattern
            transformed.append(matcher.group("end")); // copy unmodified end sequence of the pattern
            pos = matcher.end(); // continue at position at the end of the pattern
        }
        if (pos >= 0 && pos < len) {
            transformed.append(source.substring(pos)); // copy the rest of the source
        }
        return transformed.toString();
    }
}
