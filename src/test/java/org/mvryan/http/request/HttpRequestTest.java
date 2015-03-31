package org.mvryan.http.request;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.NonFinal;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mvryan.http.response.HttpResponseCode;

import com.google.common.collect.Maps;

public class HttpRequestTest
{
    private HttpRequest sut = null;
    
    @Before
    public void before()
    {
        sut = new HttpRequest();
    }
    
    @After
    public void after()
    {
        sut = null;
    }
    
    @Test
    public void testStandardGet() throws IOException
    {
        final HttpResponseCode rc = sut.parse(STD_GET.getInputStream());
        assertEquals(HttpResponseCode.OK, rc);
        assertEquals(STD_GET.getMethod(), sut.getMethod());
        assertEquals(STD_GET.getPath(), sut.getUri().getPath());
        assertTrue(equalMap(STD_GET.getHeaders(), sut.getHeaders()));
    }
    
    private boolean equalMap(final Map<String, String> lhs, final Map<String, String> rhs)
    {
        if (lhs.size() != rhs.size())
        {
            return false;
        }
        for (final Entry<String, String> entry : lhs.entrySet())
        {
            if (! entry.getValue().equals(rhs.get(entry.getKey())))
            {
                return false;
            }
        }
        return true;
    }
    
    @Value
    @NonFinal
    static class Msg
    {
        String method;
        String path;
        Map<String, String> headers;
        
        @Override
        public String toString()
        {
            final StringBuilder sb = new StringBuilder();
            sb.append(String.format("%s %s HTTP/1.1\r\n", getMethod(), getPath()));
            for (final Entry<String, String> header : getHeaders().entrySet())
            {
                sb.append(String.format("%s: %s\r\n", header.getKey(), header.getValue()));
            }
            sb.append("\r\n");
            return sb.toString();
        }
        
        public InputStream getInputStream()
        {
            try
            {
                return new ByteArrayInputStream(toString().getBytes("UTF-8"));
            }
            catch (UnsupportedEncodingException e)
            {
                return new ByteArrayInputStream(toString().getBytes());
            }
        }
    }
    
    @Value
    @EqualsAndHashCode(callSuper=true)
    static class GetMsg extends Msg
    {
        public GetMsg(final String path, final Map<String, String> headers)
        {
            super("GET", path, headers);
        }
        
        @Override
        public String toString()
        {
            return super.toString();
        }
    }
    
    private static final Map<String, String> stdHeaders;
    static {
        Map<String, String> m = Maps.newHashMap();
        m.put("Host", "localhost");
        m.put("Connection", "keep-alive");
        m.put("Cache-Control", "no-cache");
        m.put("User-Agent", "JUnit");
        m.put("Accept", "*/*");
        stdHeaders = Collections.unmodifiableMap(m);
    }
    
    private static final GetMsg STD_GET = new GetMsg("/get/test/test.html", stdHeaders);
}
