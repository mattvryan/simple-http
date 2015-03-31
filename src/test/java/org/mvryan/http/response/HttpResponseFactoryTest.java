package org.mvryan.http.response;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.BeforeClass;
import org.junit.Test;
import org.mvryan.http.modules.ConfigurationModule;
import org.mvryan.http.request.HttpRequest;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class HttpResponseFactoryTest
{
    private final Injector injector = Guice.createInjector(new AbstractModule()
    {
        @Override protected void configure() { install(new ConfigurationModule()); }
    });
    
    private static final HttpRequest mockRequest = mock(HttpRequest.class);
    
    @BeforeClass
    public static void beforeClass() throws MalformedURLException
    {
        when(mockRequest.getUri()).thenReturn(new URL("http://localhost/test.html"));
    }
    
    @Test
    public void testGetResponseReturnsStaticContentHttpResponse()
    {
        final HttpResponseFactory sut = injector.getInstance(HttpResponseFactory.class);
        final HttpResponse response = sut.getResponse(mockRequest);
        assertTrue(response instanceof StaticContentHttpResponse);
    }
}
