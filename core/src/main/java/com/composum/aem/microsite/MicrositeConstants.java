package com.composum.aem.microsite;

public interface MicrositeConstants {

    String EXT_HTML = "html";
    String SELECTOR_UPLOAD = "upload";
    String SELECTOR_EMBEDDED = "embedded";
    String PARAM_IMPORT_FILE = "archive";

    String SOURCE_ENCODING = "UTF-8";
    String INDEX_FILE = "index.html";

    // microsite state property names

    String PN_INDEX_PATH = "indexPath";
    String PN_LAST_IMPORT_TIME = "lastImportTime";
    String PN_LAST_IMPORT_FILE = "lastImportFile";
    String PN_LAST_IMPORT_SIZE = "lastImportSize";

    public static final String PN_ARCHIVE_FILE = "uploadedArchiveFile";

    public static final String PN_FILE_NAME = "fileName";

    public static final String PN_FILE_MODIFIED = "fileModified";

    public static final String PN_FILE_MIME_TYPE = "fileMimeType";
}
