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
import org.mvryan.http.modules.ConfigurationModule;
import org.mvryan.http.request.HttpRequest;
import org.mvryan.http.response.HttpResponse;
import org.mvryan.http.response.HttpResponseCode;

import com.google.common.collect.Maps;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;

public class FilesystemResponseStrategyTest
{
    private HttpResponseStrategy sut = null;
    
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
        
        Injector injector = Guice.createInjector(new AbstractModule(){
            @Override public void configure()
            {
                bind(String.class).annotatedWith(Names.named(ConfigurationModule.DOCUMENT_ROOT)).toInstance(docRoot);
                bind(String.class).annotatedWith(Names.named(ConfigurationModule.ALLOW_DIRECTORY_INDEX)).toInstance(Boolean.FALSE.toString());
                bind(FilesystemResolver.class).toInstance(resolver);
                bind(HttpResponseStrategy.class).to(FilesystemResponseStrategy.class);
            }
        });
        
        sut = injector.getInstance(FilesystemResponseStrategy.class);
    }
    
    @Test
    public void testDetermineFileResponse() throws MalformedURLException
    {
        when(mockRequest.getUri()).thenReturn(new URL("http://localhost/get/test.html"));
        HttpResponse response = sut.determineResponse( mockRequest);
        assertEquals(HttpResponseCode.OK, response.getResponseCode());
    }
    
    @Test
    public void testDetermineDefaultFileResponse() throws MalformedURLException
    {
        when(mockRequest.getUri()).thenReturn(new URL("http://localhost/get/"));
        when(resolver.getDefaultHtmlFile(any(File.class))).thenReturn(mockIndexFile);
        HttpResponse response = sut.determineResponse( mockRequest);
        assertEquals(HttpResponseCode.OK, response.getResponseCode());
    }
    
    @Test
    public void testDetermineDirectoryIndexResponse() throws MalformedURLException
    {
        when(mockRequest.getUri()).thenReturn(new URL("http://localhost/get/"));
        when(resolver.getDefaultHtmlFile(any(File.class))).thenReturn(null);
        HttpResponse response = sut.determineResponse( mockRequest);
        assertEquals(HttpResponseCode.FORBIDDEN, response.getResponseCode());
    }
    
    @Test
    public void testAccessDeniedResponse() throws MalformedURLException
    {
        when(mockRequest.getUri()).thenReturn(new URL("http://localhost/get/test.html"));
        when(mockTestFile.canRead()).thenReturn(false);
        HttpResponse response = sut.determineResponse( mockRequest);
        assertEquals(HttpResponseCode.FORBIDDEN, response.getResponseCode());
    }
    
    @Test
    public void testFileNotFoundResponse() throws MalformedURLException
    {
        when(mockRequest.getUri()).thenReturn(new URL("http://localhost/get/test.html"));
        when(mockTestFile.exists()).thenReturn(false);
        HttpResponse response = sut.determineResponse(mockRequest);
        assertEquals(HttpResponseCode.FILE_NOT_FOUND, response.getResponseCode());
    }
    
    @Test
    public void testHtmlReturnsHtmlContentType() throws IOException
    {
        when(mockRequest.getUri()).thenReturn(new URL("http://localhost/get/test.html"));
        when(resolver.resolveContentType(any(File.class))).thenReturn("text/html");
        HttpResponse response = sut.determineResponse( mockRequest);
        assertEquals("text/html", response.getContentType());
    }
    
    @Test
    public void testTextReturnsTextContentType() throws IOException
    {
        when(mockRequest.getUri()).thenReturn(new URL("http://localhost/get/test.txt"));
        when(resolver.resolveContentType(any(File.class))).thenReturn("text/plain");
        HttpResponse response = sut.determineResponse( mockRequest);
        assertEquals("text/plain", response.getContentType());
    }
    
    @Test
    public void testXmlReturnsXmlContentType() throws IOException
    {
        when(mockRequest.getUri()).thenReturn(new URL("http://localhost/get/test.xml"));
        when(resolver.resolveContentType(any(File.class))).thenReturn("application/xml");
        HttpResponse response = sut.determineResponse( mockRequest);
        assertEquals("application/xml", response.getContentType());
    }
    
    @Test
    public void testJsonReturnsJsonContentType() throws IOException
    {
        when(mockRequest.getUri()).thenReturn(new URL("http://localhost/get/test.json"));
        when(resolver.resolveContentType(any(File.class))).thenReturn("application/json");
        HttpResponse response = sut.determineResponse( mockRequest);
        assertEquals("application/json", response.getContentType());
    }
    
    @Test
    public void testOtherReturnsOctetStreamContentType() throws IOException
    {
        when(mockRequest.getUri()).thenReturn(new URL("http://localhost/get/test.jpg"));
        when(resolver.resolveContentType(any(File.class))).thenReturn("application/octet-stream");
        HttpResponse response = sut.determineResponse( mockRequest);
        assertEquals("application/octet-stream", response.getContentType());
    }
    
    @Test
    public void testResponseNotAcceptable() throws IOException
    {
        when(mockRequest.getHeaders()).thenReturn(mockHeadersRestrictedAccept);
        when(mockRequest.getUri()).thenReturn(new URL("http://localhost/get/test.html"));
        when(resolver.resolveContentType(any(File.class))).thenReturn("text/html");
        HttpResponse response = sut.determineResponse( mockRequest);
        assertEquals(HttpResponseCode.NOT_ACCEPTABLE, response.getResponseCode());
        assertEquals("text/html", new String(response.getResponsePayload()));
    }
    
    @Test
    public void testResponseNotAcceptableWithList() throws IOException
    {
        when(mockRequest.getHeaders()).thenReturn(mockHeadersRestrictedAcceptList);
        when(mockRequest.getUri()).thenReturn(new URL("http://localhost/get/test.html"));
        when(resolver.resolveContentType(any(File.class))).thenReturn("text/html");
        HttpResponse response = sut.determineResponse( mockRequest);
        assertEquals(HttpResponseCode.NOT_ACCEPTABLE, response.getResponseCode());
        assertEquals("text/html", new String(response.getResponsePayload()));
    }
    
    @Test
    public void testResponseAcceptable() throws IOException
    {
        when(mockRequest.getHeaders()).thenReturn(mockHeadersRestrictedAccept);
        when(mockRequest.getUri()).thenReturn(new URL("http://localhost/get/test.txt"));
        when(resolver.resolveContentType(any(File.class))).thenReturn("text/plain");
        HttpResponse response = sut.determineResponse( mockRequest);
        assertEquals(HttpResponseCode.OK, response.getResponseCode());
    }
    
    @Test
    public void testResponseAcceptableWithList() throws IOException
    {
        when(mockRequest.getHeaders()).thenReturn(mockHeadersRestrictedAcceptList);
        when(mockRequest.getUri()).thenReturn(new URL("http://localhost/get/test.xml"));
        when(resolver.resolveContentType(any(File.class))).thenReturn("text/xml");
        HttpResponse response = sut.determineResponse( mockRequest);
        assertEquals(HttpResponseCode.OK, response.getResponseCode());
    }
    
    @Test
    public void testResponseAcceptableWithWildcard() throws IOException
    {
        when(mockRequest.getHeaders()).thenReturn(mockHeaders);
        when(mockRequest.getUri()).thenReturn(new URL("http://localhost/get/test.html"));
        when(resolver.resolveContentType(any(File.class))).thenReturn("madeupcontenttype/fake");
        HttpResponse response = sut.determineResponse( mockRequest);
        assertEquals(HttpResponseCode.OK, response.getResponseCode());
    }
}
