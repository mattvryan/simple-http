package org.mvryan.http.request;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import lombok.Getter;

import org.mvryan.http.response.HttpResponseCode;

import com.google.common.base.Strings;
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
    
    private String readWord(final BufferedReader reader) throws IOException
    {
        char nextChar;
        while (Character.isWhitespace(nextChar = (char) reader.read()));
        if (-1 == nextChar)
        {
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
        return sb.length() > 0 ? sb.toString() : null;
    }
    
    private boolean isSupportedMethod(final String method)
    {
        return method.equals("GET");
    }
    
    private HttpResponseCode parseHeaders(final BufferedReader reader) throws IOException
    {
        String line;
        String[] parts;
        boolean emptyLineEndsParsing = false;
        
        while (null != (line = reader.readLine().trim()))
        {
            if (line.length() < 1)
            {
                if (emptyLineEndsParsing)
                {
                    break;
                }
                else
                {
                    emptyLineEndsParsing = true;
                    continue;
                }
            }
            parts = line.split(":", 2);
            if (parts.length < 2)
            {
                return HttpResponseCode.BAD_REQUEST;
            }
            headers.put(parts[0].trim(), parts[1].trim());
        }
        
        return HttpResponseCode.OK;
    }
}
