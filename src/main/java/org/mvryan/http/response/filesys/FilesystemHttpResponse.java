package org.mvryan.http.response.filesys;

import javax.inject.Inject;
import javax.inject.Named;

import lombok.Getter;

import org.mvryan.http.modules.ConfigurationModule;
import org.mvryan.http.request.HttpRequest;
import org.mvryan.http.response.HttpResponse;
import org.mvryan.http.response.HttpResponseCode;

public class FilesystemHttpResponse implements HttpResponse
{
    @Getter
    private HttpResponseCode responseCode;
    @Getter
    private byte[] responsePayload = null;
    @Getter
    private String contentType = FilesystemResponseStrategy.CONTENT_TYPE_DEFAULT; // HTTP default
    
    private final String documentRootPath;
    private final FilesystemResponseStrategy responseStrategy;
        
    @Inject
    public FilesystemHttpResponse(@Named(ConfigurationModule.DOCUMENT_ROOT) final String documentRoot,
            final FilesystemResponseStrategy responseStrategy)
    {
        responseCode = HttpResponseCode.OK;
        documentRootPath = documentRoot;
        this.responseStrategy = responseStrategy;
    }
    
    @Override
    public FilesystemHttpResponse forRequest(final HttpRequest request)
    {
        responseStrategy.determineResponse(documentRootPath, request);
        responseCode = responseStrategy.getResponseCode();
        responsePayload = responseStrategy.getResponsePayload();
        contentType = responseStrategy.getContentType();
        return this;
    }
}
