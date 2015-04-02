package org.mvryan.http.response.filesys;

import lombok.Getter;
import lombok.experimental.Builder;

import org.mvryan.http.response.HttpResponse;
import org.mvryan.http.response.HttpResponseCode;

@Builder
public class FilesystemHttpResponse implements HttpResponse
{
    @Getter
    private HttpResponseCode responseCode;
    @Getter
    private byte[] responsePayload = null;
    @Getter
    private String contentType = HttpResponseStrategy.CONTENT_TYPE_DEFAULT; // HTTP default
    
    private FilesystemHttpResponse(final HttpResponseCode responseCode,
            final byte[] responsePayload,
            final String contentType)
    {
        this.responseCode = responseCode;
        this.responsePayload = responsePayload;
        this.contentType = null != contentType ? contentType : HttpResponseStrategy.CONTENT_TYPE_DEFAULT;
    }
}
