package com.composum.aem.microsite.model;

import com.composum.aem.microsite.servlet.MicrositeServlet;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.PostConstruct;

import static com.composum.aem.microsite.MicrositeConstants.PN_INDEX_PATH;
import static com.day.cq.commons.jcr.JcrConstants.JCR_CONTENT;

@SuppressWarnings("unused")
@Model(adaptables = SlingHttpServletRequest.class)
public class MicrositeFrame {

    enum Mode {iframe, link}

    @Self
    protected SlingHttpServletRequest request;

    @ScriptVariable
    protected Resource resource;

    @ValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL)
    protected String micrositePath;

    @ValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL)
    protected String indexPath;

    @ValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL)
    protected String linkText;

    @ValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL)
    protected String linkTitle;

    @ValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL)
    protected String linkTarget;

    @ValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL)
    protected String frameWidth;

    @ValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL)
    protected String frameRatio;

    @ValueMapValue
    @Default(values = {})
    protected String[] frameSandbox;

    @ValueMapValue()
    @Default(booleanValues = false)
    protected boolean frameFullscreen;

    protected ResourceResolver resourceResolver;
    private transient Resource microsite;
    private transient String micrositeSrc;

    @PostConstruct
    protected void init() {
        resourceResolver = resource.getResourceResolver();
    }

    public @NotNull String getMode() {
        return (StringUtils.isNotBlank(linkText) ? Mode.link : Mode.iframe).name();
    }

    public String getMicrositeSrc() {
        if (micrositeSrc == null) {
            micrositeSrc = "";
            final Resource microsite = getMicrosite();
            if (microsite != null) {
                final PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
                final Page micrositePage;
                Resource pageContent;
                if (pageManager != null
                        && (micrositePage = pageManager.getContainingPage(microsite)) != null
                        && (pageContent = micrositePage.getContentResource()) != null
                        && pageContent.isResourceType(MicrositeServlet.RESOURCE_TYPE)) {
                    micrositeSrc = microsite.getPath() + ".html?wcmmode=disabled";
                } else {
                    micrositeSrc = microsite.getPath();
                    final String indexPath = getIndexPath();
                    if (StringUtils.isNotBlank(indexPath)) {
                        micrositeSrc = indexPath.startsWith("/") ? indexPath : micrositeSrc + "/" + indexPath;
                    }
                }
            }
        }
        return micrositeSrc;
    }

    public String getIndexPath() {
        if (indexPath == null) {
            indexPath = "";
            final Resource microsite = getMicrosite();
            if (microsite != null) {
                final Resource contentNode = microsite.getChild(JCR_CONTENT);
                final ValueMap properties = contentNode != null ? contentNode.getValueMap() : microsite.getValueMap();
                indexPath = properties.get(PN_INDEX_PATH, "");
            }
        }
        return indexPath;
    }

    protected @Nullable Resource getMicrosite() {
        if (microsite == null) {
            if (StringUtils.isNotBlank(micrositePath)) {
                microsite = resourceResolver.getResource(micrositePath);
            }
        }
        return microsite;
    }

    public @Nullable String getLinkText() {
        return linkText;
    }

    public @Nullable String getLinkTitle() {
        return linkTitle;
    }

    public @Nullable String getLinkTarget() {
        return StringUtils.defaultIfBlank(linkTarget, "_blank");
    }

    public @NotNull String getFrameWidth() {
        return StringUtils.defaultIfBlank(frameWidth, "");
    }

    public @NotNull String getFrameStyle() {
        return StringUtils.isNotBlank(frameRatio) ? "aspect-ratio: " + frameRatio + ";" : "";
    }

    public @NotNull String getFrameSandbox() {
        return StringUtils.join(frameSandbox, " ");
    }
}
