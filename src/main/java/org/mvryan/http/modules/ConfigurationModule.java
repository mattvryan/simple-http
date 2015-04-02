package org.mvryan.http.modules;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

@Slf4j
public class ConfigurationModule extends AbstractModule
{
    public static final String DOCUMENT_ROOT = "org.mvryan.simple-http.document-root";
    public static final String ALLOW_DIRECTORY_INDEX = "org.mvryan.simple-http.allow-directory-index";
    public static final String CACHE_ENABLED = "org.mvryan.simple-http.cache-enabled";
    
    @Getter
    private Map<String, String> config = Maps.newHashMap();
    
    @Override
    protected void configure()
    {
        final String homeDirectory = System.getProperty("user.home");
        final String docRoot = Strings.isNullOrEmpty(homeDirectory) ? "/var/www/html" :
            (homeDirectory.endsWith(File.separator) ? homeDirectory + "public_html" :
                homeDirectory + File.separator + "public_html");
        
        // Load default configuration
        config.put(DOCUMENT_ROOT, docRoot);
        config.put(ALLOW_DIRECTORY_INDEX, Boolean.FALSE.toString());
        config.put(CACHE_ENABLED, Boolean.FALSE.toString());
        
        // Load any configuration from resources
        try
        {
            final Properties resourceProperties = new Properties();
            final InputStream is = getClass().getClassLoader().getResourceAsStream("config.properties");
            if (null != is)
            {
                resourceProperties.load(this.getClass().getClassLoader().getResourceAsStream("config.properties"));
                for (final Entry<Object, Object> entry : resourceProperties.entrySet())
                {
                    config.put((String) entry.getKey(), (String) entry.getValue());
                }
            }
        }
        catch (IOException e)
        {
            log.info("Unable to load config.properties as resource");
        }

        final File cfgFile = new File("/etc/simple_http/config.properties");
        if (cfgFile.exists() && cfgFile.canRead())
        {
            final Properties cfgProperties = new Properties();
            try
            {
                cfgProperties.load(new FileInputStream(cfgFile));
                for (final Entry<Object, Object> entry : cfgProperties.entrySet())
                {
                    config.put((String) entry.getKey(), (String) entry.getValue());
                }
            }
            catch (IOException e)
            {
                log.warn("Unable to load /etc/simple_http/config.properties", e);
            }
        }
        
        for (final Entry<String, String> entry : config.entrySet())
        {
            bind(String.class).annotatedWith(Names.named(entry.getKey())).toInstance(entry.getValue());
        }
    }
}
