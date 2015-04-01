package org.mvryan.http.response.filesys;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mvryan.http.request.HttpRequest;
import org.mvryan.http.response.HttpResponseCode;

import com.google.common.collect.Maps;

public class DefaultFilesystemResponseStrategyTest
{
    private FilesystemResponseStrategy sut = null;
    
    private final FilesystemResolver resolver = mock(FilesystemResolver.class);
    private final HttpRequest mockRequest = mock(HttpRequest.class);
    private final File mockDocRoot = mock(File.class);
    private final File mockSubRoot = mock(File.class);
    private final File mockTestFile = mock(File.class);
    private final File mockIndexFile = mock(File.class);
    
    private static final String docRoot = "/docroot";
    
    private String testContent = "Test Content";
    
    private Map<String, String> mockHeaders = Maps.newHashMap();
    private Map<String, String> mockHeadersRestrictedAccept = Maps.newHashMap();
    private Map<String, String> mockHeadersRestrictedAcceptList = Maps.newHashMap();
    
    @Before
    public void before() throws IOException
    {
        when(resolver.getFile(docRoot)).thenReturn(mockDocRoot);
        when(resolver.getFile(docRoot + "/get/")).thenReturn(mockSubRoot);
        when(resolver.getFile(docRoot + "/get/test.html")).thenReturn(mockTestFile);
        when(resolver.getFile(docRoot + "/get/test.txt")).thenReturn(mockTestFile);
        when(resolver.getFile(docRoot + "/get/test.xml")).thenReturn(mockTestFile);
        when(resolver.getFile(docRoot + "/get/test.json")).thenReturn(mockTestFile);
        when(resolver.getFile(docRoot + "/get/test.jpg")).thenReturn(mockTestFile);
        when(resolver.readFileBytes(any(File.class))).thenReturn(testContent.getBytes());
        
        when(mockDocRoot.exists()).thenReturn(true);
        when(mockDocRoot.isDirectory()).thenReturn(true);
        when(mockDocRoot.canRead()).thenReturn(true);
        
        when(mockSubRoot.exists()).thenReturn(true);
        when(mockSubRoot.isDirectory()).thenReturn(true);
        when(mockSubRoot.canRead()).thenReturn(true);
        
        when(mockTestFile.exists()).thenReturn(true);
        when(mockTestFile.canRead()).thenReturn(true);
        when(mockTestFile.isDirectory()).thenReturn(false);
        when(mockTestFile.getName()).thenReturn(new String("test.html"));

        when(mockIndexFile.exists()).thenReturn(true);
        when(mockIndexFile.canRead()).thenReturn(true);
        when(mockIndexFile.isDirectory()).thenReturn(false);
        when(mockIndexFile.getName()).thenReturn("index.html");
        
        mockHeaders.put("Host", "localhost");
        mockHeaders.put("Connection", "keep-alive");
        mockHeaders.put("Cache-Control", "no-cache");
        mockHeaders.put("User-Agent", "JUnit");
        mockHeaders.put("Accept", "*/*");
        
        mockHeadersRestrictedAccept.put("Host", "localhost");
        mockHeadersRestrictedAccept.put("Connection", "keep-alive");
        mockHeadersRestrictedAccept.put("Cache-Control", "no-cache");
        mockHeadersRestrictedAccept.put("User-Agent", "JUnit");
        mockHeadersRestrictedAccept.put("Accept", "text/plain");        
        
        mockHeadersRestrictedAcceptList.put("Host", "localhost");
        mockHeadersRestrictedAcceptList.put("Connection", "keep-alive");
        mockHeadersRestrictedAcceptList.put("Cache-Control", "no-cache");
        mockHeadersRestrictedAcceptList.put("User-Agent", "JUnit");
        mockHeadersRestrictedAcceptList.put("Accept", "text/plain; UTF-8, text/xml");        
        
        sut = new DefaultFilesystemResponseStrategy(resolver);
    }
    
    @Test
    public void testDetermineFileResponse() throws MalformedURLException
    {
        when(mockRequest.getUri()).thenReturn(new URL("http://localhost/get/test.html"));
        sut.determineResponse(docRoot, mockRequest);
        assertEquals(HttpResponseCode.OK, sut.getResponseCode());
    }
    
    @Test
    public void testDetermineDefaultFileResponse() throws MalformedURLException
    {
        when(mockRequest.getUri()).thenReturn(new URL("http://localhost/get/"));
        when(resolver.getDefaultHtmlFile(any(File.class))).thenReturn(mockIndexFile);
        sut.determineResponse(docRoot, mockRequest);
        assertEquals(HttpResponseCode.OK, sut.getResponseCode());
    }
    
    @Test
    public void testDetermineDirectoryIndexResponse() throws MalformedURLException
    {
        when(mockRequest.getUri()).thenReturn(new URL("http://localhost/get/"));
        when(resolver.getDefaultHtmlFile(any(File.class))).thenReturn(null);
        sut.determineResponse(docRoot, mockRequest);
        assertEquals(HttpResponseCode.FORBIDDEN, sut.getResponseCode());
    }
    
    @Test
    public void testAccessDeniedResponse() throws MalformedURLException
    {
        when(mockRequest.getUri()).thenReturn(new URL("http://localhost/get/test.html"));
        when(mockTestFile.canRead()).thenReturn(false);
        sut.determineResponse(docRoot, mockRequest);
        assertEquals(HttpResponseCode.FORBIDDEN, sut.getResponseCode());
    }
    
    @Test
    public void testFileNotFoundResponse() throws MalformedURLException
    {
        when(mockRequest.getUri()).thenReturn(new URL("http://localhost/get/test.html"));
        when(mockTestFile.exists()).thenReturn(false);
        sut.determineResponse(docRoot, mockRequest);
        assertEquals(HttpResponseCode.FILE_NOT_FOUND, sut.getResponseCode());
    }
    
    @Test
    public void testHtmlReturnsHtmlContentType() throws IOException
    {
        when(mockRequest.getUri()).thenReturn(new URL("http://localhost/get/test.html"));
        when(resolver.resolveContentType(any(File.class))).thenReturn("text/html");
        sut.determineResponse(docRoot, mockRequest);
        assertEquals("text/html", sut.getContentType());
    }
    
    @Test
    public void testTextReturnsTextContentType() throws IOException
    {
        when(mockRequest.getUri()).thenReturn(new URL("http://localhost/get/test.txt"));
        when(resolver.resolveContentType(any(File.class))).thenReturn("text/plain");
        sut.determineResponse(docRoot, mockRequest);
        assertEquals("text/plain", sut.getContentType());
    }
    
    @Test
    public void testXmlReturnsXmlContentType() throws IOException
    {
        when(mockRequest.getUri()).thenReturn(new URL("http://localhost/get/test.xml"));
        when(resolver.resolveContentType(any(File.class))).thenReturn("application/xml");
        sut.determineResponse(docRoot, mockRequest);
        assertEquals("application/xml", sut.getContentType());
    }
    
    @Test
    public void testJsonReturnsJsonContentType() throws IOException
    {
        when(mockRequest.getUri()).thenReturn(new URL("http://localhost/get/test.json"));
        when(resolver.resolveContentType(any(File.class))).thenReturn("application/json");
        sut.determineResponse(docRoot, mockRequest);
        assertEquals("application/json", sut.getContentType());
    }
    
    @Test
    public void testOtherReturnsOctetStreamContentType() throws IOException
    {
        when(mockRequest.getUri()).thenReturn(new URL("http://localhost/get/test.jpg"));
        when(resolver.resolveContentType(any(File.class))).thenReturn("application/octet-stream");
        sut.determineResponse(docRoot, mockRequest);
        assertEquals("application/octet-stream", sut.getContentType());
    }
    
    @Test
    public void testResponseNotAcceptable() throws IOException
    {
        when(mockRequest.getHeaders()).thenReturn(mockHeadersRestrictedAccept);
        when(mockRequest.getUri()).thenReturn(new URL("http://localhost/get/test.html"));
        when(resolver.resolveContentType(any(File.class))).thenReturn("text/html");
        sut.determineResponse(docRoot, mockRequest);
        assertEquals(HttpResponseCode.NOT_ACCEPTABLE, sut.getResponseCode());
        assertEquals("text/html", new String(sut.getResponsePayload()));
    }
    
    @Test
    public void testResponseNotAcceptableWithList() throws IOException
    {
        when(mockRequest.getHeaders()).thenReturn(mockHeadersRestrictedAcceptList);
        when(mockRequest.getUri()).thenReturn(new URL("http://localhost/get/test.html"));
        when(resolver.resolveContentType(any(File.class))).thenReturn("text/html");
        sut.determineResponse(docRoot, mockRequest);
        assertEquals(HttpResponseCode.NOT_ACCEPTABLE, sut.getResponseCode());
        assertEquals("text/html", new String(sut.getResponsePayload()));
    }
    
    @Test
    public void testResponseAcceptable() throws IOException
    {
        when(mockRequest.getHeaders()).thenReturn(mockHeadersRestrictedAccept);
        when(mockRequest.getUri()).thenReturn(new URL("http://localhost/get/test.txt"));
        when(resolver.resolveContentType(any(File.class))).thenReturn("text/plain");
        sut.determineResponse(docRoot, mockRequest);
        assertEquals(HttpResponseCode.OK, sut.getResponseCode());
    }
    
    @Test
    public void testResponseAcceptableWithList() throws IOException
    {
        when(mockRequest.getHeaders()).thenReturn(mockHeadersRestrictedAcceptList);
        when(mockRequest.getUri()).thenReturn(new URL("http://localhost/get/test.xml"));
        when(resolver.resolveContentType(any(File.class))).thenReturn("text/xml");
        sut.determineResponse(docRoot, mockRequest);
        assertEquals(HttpResponseCode.OK, sut.getResponseCode());
    }
    
    @Test
    public void testResponseAcceptableWithWildcard() throws IOException
    {
        when(mockRequest.getHeaders()).thenReturn(mockHeaders);
        when(mockRequest.getUri()).thenReturn(new URL("http://localhost/get/test.html"));
        when(resolver.resolveContentType(any(File.class))).thenReturn("madeupcontenttype/fake");
        sut.determineResponse(docRoot, mockRequest);
        assertEquals(HttpResponseCode.OK, sut.getResponseCode());
    }
}
