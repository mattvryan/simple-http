package org.mvryan.http.modules;

import org.mvryan.http.response.filesys.FilesystemResponseStrategy;
import org.mvryan.http.response.filesys.HttpResponseStrategy;

import com.google.inject.AbstractModule;

public class HttpServerModule extends AbstractModule
{
    @Override
    protected void configure()
    {
        install(new ConfigurationModule());        
        bind(HttpResponseStrategy.class).to(FilesystemResponseStrategy.class);
    }
}
