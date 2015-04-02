package org.mvryan.http.response;


public interface HttpResponse
{
    HttpResponseCode getResponseCode();
    byte[] getResponsePayload();
    String getContentType();
}
