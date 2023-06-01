package com.composum.aem.microsite.service.impl;

import com.composum.aem.commons.servlet.Status;
import com.composum.aem.microsite.service.MicrositeImportService;
import com.composum.aem.microsite.servlet.MicrositeServlet;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.ModificationType;
import org.apache.sling.servlets.post.SlingPostProcessor;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipInputStream;

import static com.composum.aem.microsite.MicrositeConstants.PN_ARCHIVE_FILE;
import static com.composum.aem.microsite.MicrositeConstants.PN_FILE_MIME_TYPE;
import static com.composum.aem.microsite.MicrositeConstants.PN_FILE_MODIFIED;

@Component(service = SlingPostProcessor.class, immediate = true)
public class MicrositeUploadProcessor implements SlingPostProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(MicrositeUploadProcessor.class);

    public static final Pattern FILE_PROPERTY_PATTERN = Pattern.compile(
            "^(?<path>(?<content>(.*/jcr:content))/" + PN_ARCHIVE_FILE + "\\.sftmp)(/(?<property>(.*)))?$");

    public static final Map<String, String> FILE_PROPERTIES = new HashMap<>() {{
        put("jcr:content/jcr:lastModified", PN_FILE_MODIFIED);
        put("jcr:content/jcr:mimeType", PN_FILE_MIME_TYPE);
    }};

    @Reference
    protected MicrositeImportService importService;

    protected boolean processChange(@NotNull final SlingHttpServletRequest request,
                                    @NotNull final Modification modification, @NotNull final ModificationType type,
                                    @NotNull final BiConsumer<Matcher, Resource> adjustment) {
        final Matcher matcher;
        if (modification.getType() == type
                && (matcher = FILE_PROPERTY_PATTERN.matcher(modification.getSource())).matches()) {
            final ResourceResolver resolver = request.getResourceResolver();
            final Resource content = resolver.getResource(matcher.group("content"));
            if (content != null && content.isResourceType(MicrositeServlet.RESOURCE_TYPE)) {
                adjustment.accept(matcher, content);
                return true;
            }
        }
        return false;
    }

    @Override
    public void process(SlingHttpServletRequest request, List<Modification> list) {
        for (Modification modification : list) {
            if (!processChange(request, modification, ModificationType.MODIFY, (matcher, content) -> {
                final Resource node = content.getResourceResolver().getResource(matcher.group("path"));
                final String srcName;
                final String dstName;
                if (node != null
                        && StringUtils.isNotBlank(srcName = matcher.group("property"))
                        && (dstName = FILE_PROPERTIES.get(srcName)) != null) {
                    final ModifiableValueMap valueMap = content.adaptTo(ModifiableValueMap.class);
                    if (valueMap != null) {
                        valueMap.put(dstName, node.getValueMap().get(srcName));
                    }
                }
            })) {
                processChange(request, modification, ModificationType.MOVE, (matcher, content) -> {
                    final Resource archive = content.getResourceResolver()
                            .getResource(modification.getDestination() + "/" + JcrConstants.JCR_CONTENT);
                    if (archive != null) {
                        final ValueMap properties = archive.getValueMap();
                        try (final ZipInputStream zipStream = Optional.ofNullable(properties
                                        .get(JcrConstants.JCR_DATA, InputStream.class))
                                .map(ZipInputStream::new)
                                .orElse(null)) {
                            if (zipStream != null) {
                                final Status status = new Status(request, null);
                                importService.importSiteContent(status, content, zipStream);
                            }
                        } catch (IOException ex) {
                            LOG.error(ex.getMessage(), ex);
                        }
                    }
                });
            }
        }
    }
}
