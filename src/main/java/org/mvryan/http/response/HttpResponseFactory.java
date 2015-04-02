package org.mvryan.http.response;

import javax.inject.Inject;

import org.mvryan.http.request.HttpRequest;
import org.mvryan.http.response.filesys.HttpResponseStrategy;

import com.google.inject.Injector;

public class HttpResponseFactory
{
    final Injector injector;
    
    // I prefer making this an instance method to letting it be static.
    // Constructing an instance means that I could set up a mapping
    // table to map URIs in the request to HttpResponse classes
    // to make the response factory more extensible.  That mapping
    // could even be provided via dependency injection.
    //
    // Not needed for a simple file-based server though.
    
    @Inject
    public HttpResponseFactory(Injector injector)
    {
        this.injector = injector;
    }
    
    public HttpResponseStrategy getResponseStrategy(final HttpRequest request)
    {
        return injector.getInstance(HttpResponseStrategy.class);
    }
}
