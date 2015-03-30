package org.mvryan.http.modules;

import java.io.File;

import com.google.common.base.Strings;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

public class ConfigurationModule extends AbstractModule
{
    public static final String DOCUMENT_ROOT = "DocumentRoot";
    
    @Override
    protected void configure()
    {
        final String homeDirectory = System.getProperty("user.home");
        final String docRoot = Strings.isNullOrEmpty(homeDirectory) ? "/var/www/html" :
            (homeDirectory.endsWith(File.separator) ? homeDirectory + "public_html" :
                homeDirectory + File.separator + "public_html");
        bind(String.class).annotatedWith(Names.named(DOCUMENT_ROOT)).toInstance(docRoot);
    }
}
