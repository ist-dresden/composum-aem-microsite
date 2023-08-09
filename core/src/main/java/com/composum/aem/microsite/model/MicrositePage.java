package com.composum.aem.microsite.model;

import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.wcm.api.Page;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import static com.composum.aem.microsite.MicrositeConstants.EXT_HTML;
import static com.composum.aem.microsite.MicrositeConstants.PN_ARCHIVE_FILE;
import static com.composum.aem.microsite.MicrositeConstants.SELECTOR_EMBEDDED;
import static com.composum.aem.microsite.MicrositeConstants.SELECTOR_UPLOAD;

@SuppressWarnings("unused")
@Model(adaptables = SlingHttpServletRequest.class)
public class MicrositePage {

    @Self
    protected SlingHttpServletRequest request;

    @ScriptVariable
    protected Page currentPage;

    @ScriptVariable
    protected ValueMap pageProperties;

    /**
     * @return the URL for the POST action to upload a ZIP file as content of the Microsite
     */
    public String getContentUploadUrl() {
        return request.getContextPath() + currentPage.getPath() + "/" + JcrConstants.JCR_CONTENT + "." + SELECTOR_UPLOAD + "." + EXT_HTML;
    }

    public String getEmbeddedPreviewUrl() {
        return request.getContextPath() + currentPage.getPath() + "." + SELECTOR_EMBEDDED + "." + EXT_HTML;
    }

    public String getName() {
        return currentPage.getName();
    }

    public String getTitle() {
        return currentPage.getTitle();
    }

    public ValueMap getProperties() {
        return pageProperties;
    }

    public String getFileName() {
        return pageProperties.get("fileName", "");
    }

    public String getDownloadName() {
        String fileName = getFileName();
        return StringUtils.isNotBlank(fileName) ? fileName : "archiveFile.zip";
    }

    public String getFileModified() {
        return toString(pageProperties.get("fileModified", Calendar.class));
    }

    public String getArchiveFileUri() {
        return currentPage.getPath() + "/" + JcrConstants.JCR_CONTENT + "/" + PN_ARCHIVE_FILE;
    }

    public String getLastImportTime() {
        return toString(pageProperties.get("lastImportTime", Calendar.class));
    }

    public @NotNull String toString(@Nullable final Calendar calendar) {
        return calendar != null ? new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(calendar.getTime()) : "";
    }
}
