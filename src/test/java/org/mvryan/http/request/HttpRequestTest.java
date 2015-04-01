package org.mvryan.http.request;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import lombok.AllArgsConstructor;
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
    
    @Test
    public void testUnsupportedMethod() throws IOException
    {
        final HttpResponseCode rc = sut.parse(UNSUPPORTED_METHOD.getInputStream());
        assertEquals(HttpResponseCode.NOT_IMPLEMENTED, rc);
    }
    
    @Test
    public void testMalformedUri() throws IOException
    {
        final HttpResponseCode rc = sut.parse(MALFORMED_URI_GET.getInputStream());
        assertEquals(HttpResponseCode.BAD_REQUEST, rc);
    }
    
    @Test
    public void testUnsupportedVersion10() throws IOException
    {
        final HttpResponseCode rc = sut.parse(VERSION_1_0_GET.getInputStream());
        assertEquals(HttpResponseCode.HTTP_VERSION_NOT_SUPPORTED, rc);
    }
    
    @Test
    public void testUnsupportedVersion20() throws IOException
    {
        final HttpResponseCode rc = sut.parse(VERSION_2_0_GET.getInputStream());
        assertEquals(HttpResponseCode.HTTP_VERSION_NOT_SUPPORTED, rc);
    }
    
    @Test
    public void testUnsupportedVersionOther() throws IOException
    {
        final HttpResponseCode rc = sut.parse(VERSION_1_2_GET.getInputStream());
        assertEquals(HttpResponseCode.HTTP_VERSION_NOT_SUPPORTED, rc);
    }
    
    @Test
    public void testUriTooLong() throws IOException
    {
        final HttpResponseCode rc = sut.parse(LONG_URI_GET.getInputStream());
        assertEquals(HttpResponseCode.REQUEST_URI_TOO_LONG, rc);
    }
    
    @Test
    public void testMissingHostHeader() throws IOException
    {
        final HttpResponseCode rc = sut.parse(MISSING_HOST_GET.getInputStream());
        assertEquals(HttpResponseCode.BAD_REQUEST, rc);
    }
    
    @Test
    public void testKeepAlive() throws IOException
    {
        sut.parse(STD_GET.getInputStream());
        assertTrue(sut.isKeepalive());
        sut.parse(NO_KEEPALIVE_GET.getInputStream());
        assertFalse(sut.isKeepalive());
    }
    
    @Test
    public void testParseRelativeUri() throws IOException
    {
        sut.parse(STD_GET.getInputStream());
        assertTrue("http://localhost/get/test/test.html".equals(sut.getUri().toString()));
    }
    
    @Test
    public void testParseFullUri() throws IOException
    {
        sut.parse(FULL_URI_GET.getInputStream());
        assertTrue("http://localhost/get/test/test.html".equals(sut.getUri().toString()));
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
    @AllArgsConstructor
    static class Msg
    {
        String method;
        String path;
        String version;
        Map<String, String> headers;
        
        public Msg(final String method, final String path, final Map<String, String> headers)
        {
            this(method, path, "1.1", headers);
        }
        
        @Override
        public String toString()
        {
            final StringBuilder sb = new StringBuilder();
            sb.append(String.format("%s %s HTTP/%s\r\n", getMethod(), getPath(), getVersion()));
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
        public GetMsg(final String path, final String version, final Map<String, String> headers)
        {
            super("GET", path, version, headers);
        }
        
        public GetMsg(final String path, final Map<String, String> headers)
        {
            this(path, "1.1", headers);
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
    
    private static final Map<String, String> noHostHeaders;
    static {
        Map<String, String> m = Maps.newHashMap();
        m.put("Connection", "keep-alive");
        m.put("Cache-Control", "no-cache");
        m.put("User-Agent", "JUnit");
        m.put("Accept", "*/*");
        noHostHeaders = Collections.unmodifiableMap(m);
    }

    private static final Map<String, String> noKeepaliveHeaders;
    static {
        Map<String, String> m = Maps.newHashMap();
        m.put("Host", "localhost");
        m.put("Connection", "close");
        m.put("Cache-Control", "no-cache");
        m.put("User-Agent", "JUnit");
        m.put("Accept", "*/*");
        noKeepaliveHeaders = Collections.unmodifiableMap(m);
    }

    private static String getLongString(final String base, int minLength)
    {
        final StringBuilder sb = new StringBuilder();
        while (sb.length() < minLength)
        {
            sb.append(base);
        }
        return sb.toString();
    }
    
    private static final Msg STD_GET = new GetMsg("/get/test/test.html", stdHeaders);
    private static final Msg UNSUPPORTED_METHOD = new Msg("LIKE", "/get/test/test.html", stdHeaders);
    private static final Msg MALFORMED_URI_GET = new GetMsg("not(a_uri)", stdHeaders);
    private static final Msg FULL_URI_GET = new GetMsg("http://localhost/get/test/test.html", stdHeaders);
    private static final Msg VERSION_1_0_GET = new GetMsg("/get/test/test.html", "1.0", stdHeaders);
    private static final Msg VERSION_1_2_GET = new GetMsg("/get/test/test.html", "1.2", stdHeaders);
    private static final Msg VERSION_2_0_GET = new GetMsg("/get/test/test.html", "2.0", stdHeaders);
    private static final Msg LONG_URI_GET = new GetMsg(getLongString("/abcdef/ghijkl/mnopqr/stu/vwx/yz", 2500), stdHeaders);
    private static final Msg MISSING_HOST_GET = new GetMsg("/get/test/test.html", noHostHeaders);
    private static final Msg NO_KEEPALIVE_GET = new GetMsg("/get/test/test.html", noKeepaliveHeaders);
}
