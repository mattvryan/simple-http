package org.mvryan.http.response.filesys;

import org.mvryan.http.request.HttpRequest;
import org.mvryan.http.response.HttpResponseCode;

public interface FilesystemResponseStrategy
{
    public static final String CONTENT_TYPE_DEFAULT = "application/octet-stream";
    public static final String CONTENT_TYPE_TEXT_HTML = "text/html";
    public static final String CONTENT_TYPE_TEXT_PLAIN = "text/plain";
    public static final String CONTENT_TYPE_APP_XML = "application/xml";
    public static final String CONTENT_TYPE_APP_JSON = "application/json";
    
    void determineResponse(final String documentRootPath, final HttpRequest request);
    HttpResponseCode getResponseCode();
    byte[] getResponsePayload();
    String getContentType();
}
