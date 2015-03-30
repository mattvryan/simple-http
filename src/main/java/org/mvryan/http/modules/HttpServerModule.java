package org.mvryan.http.modules;

import com.google.inject.AbstractModule;

public class HttpServerModule extends AbstractModule
{
    @Override
    protected void configure()
    {
        install(new ConfigurationModule());
    }
}
