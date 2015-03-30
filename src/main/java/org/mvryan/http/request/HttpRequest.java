package org.mvryan.http.request;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import lombok.Getter;

import org.mvryan.http.response.HttpResponseCode;

import com.google.common.collect.Maps;

public class HttpRequest
{
    @Getter
    private String method = null;
    @Getter
    private String uri = null;
    @Getter
    private Map<String, String> headers = Maps.newHashMap();
    
    public HttpResponseCode parse(final InputStream is) throws IOException
    {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        
        method = readWord(reader);
        if (null == method)
        {
            return HttpResponseCode.BAD_REQUEST;
        }
        else if (! isSupportedMethod(method))
        {
            return HttpResponseCode.NOT_IMPLEMENTED;
        }
        
        uri = readWord(reader);
        if (null == method)
        {
            return HttpResponseCode.BAD_REQUEST;
        }
        
        final String version = readWord(reader);
        if (null == version)
        {
            return HttpResponseCode.BAD_REQUEST;
        }
        else if (! version.equals("HTTP/1.1"))
        {
            return HttpResponseCode.HTTP_VERSION_NOT_SUPPORTED;
        }
        
        return parseHeaders(reader);
    }
    
    public boolean isKeepalive()
    {
        return false; // Not currently supported
        //final String connection = headers.get("Connection");
        //return null != connection && connection.equals("keep-alive");
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
            headers.put(header.substring(0, header.length()-1), value.trim());
        }
        
        return HttpResponseCode.OK;
    }
}
