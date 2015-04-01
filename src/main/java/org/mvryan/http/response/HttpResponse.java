package org.mvryan.http.response;

import org.mvryan.http.request.HttpRequest;

public interface HttpResponse
{
    HttpResponse forRequest(final HttpRequest request);
    HttpResponseCode getResponseCode();
    byte[] getResponsePayload();
    String getContentType();
}
