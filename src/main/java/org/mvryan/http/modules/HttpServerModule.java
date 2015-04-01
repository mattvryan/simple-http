package org.mvryan.http.modules;

import org.mvryan.http.response.filesys.DefaultFilesystemResponseStrategy;
import org.mvryan.http.response.filesys.FilesystemResponseStrategy;

import com.google.inject.AbstractModule;

public class HttpServerModule extends AbstractModule
{
    @Override
    protected void configure()
    {
        install(new ConfigurationModule());
        bind(FilesystemResponseStrategy.class).to(DefaultFilesystemResponseStrategy.class);
    }
}
