package org.mvryan.http.request;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import javax.inject.Inject;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.mvryan.http.response.HttpResponseCode;

import com.google.common.collect.Maps;

@Slf4j
public class HttpRequest
{
    @Getter
    private String method = null;
    @Getter
    private URL uri = null;
    @Getter
    private Map<String, String> headers = Maps.newHashMap();
    
    public static final String HTTP_VERSION_1_1 = "HTTP/1.1";
    public static final int MAX_URI_LENGTH=2048; // Semi-arbitrary limit with some de-facto basis.  Ask the internet for more info... :)
    private static final int MAX_WORD_LENGTH=MAX_URI_LENGTH*2;
    
    @Inject
    public HttpRequest() { }
    
    public HttpResponseCode parse(final InputStream is) throws IOException
    {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        
        method = readWord(reader);
        log.debug(String.format("Parsed request method \"%s\"", method));
        if (null == method)
        {
            return HttpResponseCode.BAD_REQUEST;
        }
        else if (! isSupportedMethod(method))
        {
            return HttpResponseCode.NOT_IMPLEMENTED;
        }
        
        final String uri = readWord(reader);
        log.debug(String.format("Parsed request URI \"%s\"", uri));
        if (null == uri)
        {
            return HttpResponseCode.BAD_REQUEST;
        }
        else if (uri.length() > MAX_URI_LENGTH)
        {
            return HttpResponseCode.REQUEST_URI_TOO_LONG;
        }
        
        final String version = readWord(reader);
        log.debug(String.format("Parsed request version \"%s\"", version));
        if (null == version)
        {
            return HttpResponseCode.BAD_REQUEST;
        }
        else if (! HTTP_VERSION_1_1.equals(version))
        {
            return HttpResponseCode.HTTP_VERSION_NOT_SUPPORTED;
        }
        
        HttpResponseCode responseCode = parseHeaders(reader);
        if (HttpResponseCode.OK != responseCode)
        {
            return responseCode;
        }
        
        final String hostHeader = headers.get("Host");
        if (null == hostHeader)
        {
            return HttpResponseCode.BAD_REQUEST;
        }
        
        responseCode = normalizePath(uri, hostHeader);
        
        return responseCode;
    }
    
    public boolean isKeepalive()
    {
        final String connection = headers.get("Connection");
        return null != connection && connection.equals("keep-alive");
    }
    
    private String readWord(final BufferedReader reader) throws IOException
    {
        char nextChar = (char) reader.read();
        if (-1 == nextChar)
        {
            return null;
        }
        else if ('\r' == nextChar)
        {
            reader.read(); // consume CRLF sequence
            return null;
        }
        
        final StringBuilder sb = new StringBuilder();
        sb.append(nextChar);
        while (! Character.isWhitespace(nextChar = (char) reader.read()))
        {
            if (-1 == nextChar)
            {
                break;
            }
            
            sb.append(nextChar);
            
            if (sb.length() > MAX_WORD_LENGTH)
            {
                // Something weird happening here
                return null;
            }
        }
        
        if ('\r' == nextChar)
        {
            reader.read(); // consume CRLF sequence
        }
        
        return sb.length() > 0 ? sb.toString() : null;
    }
    
    private boolean isSupportedMethod(final String method)
    {
        return method.equals("GET");
    }
    
    private HttpResponseCode parseHeaders(final BufferedReader reader) throws IOException
    {
        String header;
        String value;
        while (null != (header = readWord(reader)))
        {
            value = reader.readLine();
            if (null == value)
            {
                return HttpResponseCode.BAD_REQUEST;
            }
            final String h = header.substring(0, header.length()-1);
            final String v = value.trim();
            log.debug(String.format("Parsed header \"%s: %s\"", h, v));
            headers.put(h, v);
        }
        
        return HttpResponseCode.OK;
    }
    
    private HttpResponseCode normalizePath(final String requestUri, final String host)
    {
        String fullUri;
        if (requestUri.startsWith("/"))
        {
            fullUri = "http://" + host + requestUri;
        }
        else
        {
            fullUri = requestUri;
        }
        
        if (fullUri.length() > MAX_URI_LENGTH)
        {
            return HttpResponseCode.REQUEST_URI_TOO_LONG;
        }
        
        try
        {
            log.debug(String.format("Normalized request URI \"%s\", host header \"%s\" to URI \"%s\"", requestUri, host, fullUri));
            uri = new URL(fullUri);
        }
        catch (MalformedURLException e)
        {
            return HttpResponseCode.BAD_REQUEST;
        }
        
        return HttpResponseCode.OK;
    }
}
