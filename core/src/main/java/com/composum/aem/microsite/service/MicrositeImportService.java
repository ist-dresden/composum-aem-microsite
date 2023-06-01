package com.composum.aem.microsite.service;

import com.composum.aem.commons.servlet.Status;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.zip.ZipInputStream;

/**
 * The service to import a 'full' site as ZIP content into the content resource of a page.
 */
@SuppressWarnings("UnusedReturnValue")
public interface MicrositeImportService {

    /**
     * imports the input stream of a POST request parameter as ZIP stream (must be such one)
     * into a pages content resource using CRUD operations of the resolver; no commit is made.
     */
    @Nullable ModifiableValueMap importSiteContent(@NotNull Status status,
                                                   @NotNull Resource pageContent,
                                                   @NotNull RequestParameter fileParam);

    @Nullable ModifiableValueMap importSiteContent(@NotNull Status status,
                                                   @NotNull Resource pageContent,
                                                   @NotNull ZipInputStream zipStream);
}
