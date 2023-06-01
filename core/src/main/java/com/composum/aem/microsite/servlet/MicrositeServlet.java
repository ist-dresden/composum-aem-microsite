package com.composum.aem.microsite.servlet;

import com.composum.aem.commons.servlet.Status;
import com.composum.aem.microsite.service.MicrositeImportService;
import com.day.cq.wcm.api.WCMMode;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.vault.util.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.xss.XSSAPI;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.composum.aem.microsite.MicrositeConstants.*;

/**
 * The servlet of a microsite page (microsite root) implements all features
 * to import (upload) the content AND to deliver the content of the microsite pages.
 */
@Component(service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum AEM Microsite Servlet",
                ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES + "=" + MicrositeServlet.RESOURCE_TYPE,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_POST,
                ServletResolverConstants.SLING_SERVLET_EXTENSIONS + "=html",
                ServletResolverConstants.SLING_SERVLET_EXTENSIONS + "=json"
        }, immediate = true)
public class MicrositeServlet extends SlingAllMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(MicrositeServlet.class);

    public static final String RESOURCE_TYPE = "composum/aem/microsite";
    public static final String EDIT_RESOURCE_TYPE = "composum/aem/microsite/edit";

    @Reference
    protected XSSAPI xssApi;

    @Reference
    protected MicrositeImportService importService;

    protected BundleContext bundleContext;

    @Activate
    private void activate(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    /**
     * Is delivering the HTML files of the site referenced as suffix of the site page.
     */
    @Override
    public void doGet(@NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response)
            throws IOException, ServletException {
        RequestPathInfo pathInfo = request.getRequestPathInfo();
        List<String> selectors = Arrays.asList(pathInfo.getSelectors());
        String suffix = xssApi.getValidHref(pathInfo.getSuffix());
        boolean embedded = selectors.contains(SELECTOR_EMBEDDED);
        if (!embedded && WCMMode.fromRequest(request) == WCMMode.EDIT && StringUtils.isBlank(suffix)) {
            forwardForEditing(request, response);
        } else {
            if (StringUtils.isNotBlank(suffix)) {
                suffix = suffix.substring(1); /* skip leading '/' */
            }
            Resource pageContent = request.getResource();
            ValueMap pageProperties = pageContent.getValueMap();
            String indexPath = pageProperties.get(PN_INDEX_PATH, "");
            Resource contentResource = null;
            if (StringUtils.isNotBlank(suffix)) {
                contentResource = pageContent.getChild(suffix);
                if (contentResource == null && StringUtils.isNotBlank(indexPath)) {
                    String intermediatePath = StringUtils.substringBeforeLast(indexPath, "/");
                    if (StringUtils.isNotBlank(intermediatePath)) {
                        contentResource = pageContent.getChild(intermediatePath + suffix);
                    }
                }
            } else {
                if (StringUtils.isNotBlank(indexPath)) {
                    contentResource = pageContent.getChild(indexPath);
                }
            }
            if (contentResource != null && contentResource.isResourceType(JcrConstants.NT_FILE)) {
                contentResource = contentResource.getChild(JcrConstants.JCR_CONTENT);
            }
            if (contentResource != null) {
                ValueMap properties = contentResource.getValueMap();
                InputStream contentStream = properties.get(JcrConstants.JCR_DATA, InputStream.class);
                if (contentStream != null) {
                    response.setContentType(properties.get(JcrConstants.JCR_MIMETYPE, "text/html"));
                    IOUtils.copy(contentStream, response.getOutputStream());
                } else {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                }
            } else {
                if (embedded) {
                    response.setContentLength(0);
                    response.setStatus(HttpServletResponse.SC_OK);
                } else {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                }
            }
        }
    }

    /**
     * Is importing ZIP files as site content into the content resource of the microsite page.
     */
    @Override
    public void doPost(@NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response)
            throws IOException, ServletException {
        final Resource resource = request.getResource();
        final RequestPathInfo pathInfo = request.getRequestPathInfo();
        List<String> selectors = Arrays.asList(pathInfo.getSelectors());
        if (selectors.contains(SELECTOR_UPLOAD) && WCMMode.fromRequest(request) == WCMMode.EDIT) {
            Status status = new Status(request, response);
            RequestParameter importFile;
            importFile = request.getRequestParameter(PARAM_IMPORT_FILE);
            if (importFile != null) {
                importService.importSiteContent(status, request.getResource(), importFile);
                if (status.isSuccess()) {
                    request.getResourceResolver().commit();
                    status.setStatus(HttpServletResponse.SC_CREATED);
                } else {
                    status.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                }
            } else {
                status.error("no file to upload");
            }
            if (EXT_HTML.equals(pathInfo.getExtension())) {
                final String path = (JcrConstants.JCR_CONTENT.equals(resource.getName())
                        ? Objects.requireNonNull(resource.getParent()) : resource).getPath();
                response.sendRedirect(request.getContextPath() + path + "." + EXT_HTML);
            } else {
                status.sendJson();
            }
        } else {
            // forward to normal POST handling (e.g. for the default edit dialog of a page)...
            forwardForEditing(request, response);
        }
    }

    protected void forwardForEditing(@NotNull final SlingHttpServletRequest request,
                                     @NotNull final SlingHttpServletResponse response)
            throws ServletException, IOException {
        final RequestDispatcher dispatcher = getEditDispatcher(request);
        if (dispatcher != null) {
            dispatcher.forward(request, response);
        }
    }

    protected RequestDispatcher getEditDispatcher(@NotNull final SlingHttpServletRequest request) {
        Resource resource = request.getResource();
        final RequestDispatcherOptions options = new RequestDispatcherOptions();
        options.setForceResourceType(EDIT_RESOURCE_TYPE);
        final RequestDispatcher dispatcher = request.getRequestDispatcher(resource, options);
        if (dispatcher == null) {
            LOG.error("no dispatcher available for '{}'", request.getResource().getPath());
        }
        return dispatcher;
    }
}
