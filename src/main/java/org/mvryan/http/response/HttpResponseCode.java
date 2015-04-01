package org.mvryan.http.response;

import lombok.Value;

@Value
public class HttpResponseCode
{
    public static final HttpResponseCode OK =
            new HttpResponseCode(200, "OK");
    
    public static final HttpResponseCode BAD_REQUEST =
            new HttpResponseCode(400, "Bad Request");
    public static final HttpResponseCode FORBIDDEN =
            new HttpResponseCode(403, "Forbidden");
    public static final HttpResponseCode FILE_NOT_FOUND =
            new HttpResponseCode(404, "File Not Found");
    public static final HttpResponseCode NOT_ACCEPTABLE =
            new HttpResponseCode(406, "Not Acceptable");
    public static final HttpResponseCode REQUEST_URI_TOO_LONG =
            new HttpResponseCode(414, "Request-URI Too Long");
    
    public static final HttpResponseCode INTERNAL_SERVER_ERROR =
            new HttpResponseCode(500, "Internal Server Error");
    public static final HttpResponseCode NOT_IMPLEMENTED =
            new HttpResponseCode(501, "Not Implemented");
    public static final HttpResponseCode HTTP_VERSION_NOT_SUPPORTED =
            new HttpResponseCode(505, "HTTP Version Not Supported");
    
    int status;
    String reason;
    
    public boolean isRedirect()
    {
        return status >= 300 && status < 400;
    }
    
    public boolean isClientError()
    {
        return status >= 400 && status < 500;
    }
    
    public boolean isServerError()
    {
        return status >= 500;
    }
    
    public boolean isError()
    {
        return isClientError() || isServerError();
    }
}
