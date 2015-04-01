package org.mvryan.http.response.filesys;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import com.google.common.collect.Lists;

public class FilesystemResolver
{
    private final List<String> defaultHtmlDocs =
            Lists.newArrayList("index.html", "index.htm", "default.html", "default.htm");
    
    public File getFile(final String path)
    {
        return new File(path);
    }
    
    public byte[] readFileBytes(final File file) throws IOException
    {
        return Files.readAllBytes(file.toPath());
    }
    
    public File getDefaultHtmlFile(final File directory)
    {
        File defaultHtmlFile = null;
        for (final String defaultHtml : defaultHtmlDocs)
        {
            defaultHtmlFile = getFile(joinPath(directory.getAbsolutePath(), defaultHtml));
            if (defaultHtmlFile.exists() && defaultHtmlFile.isFile())
            {
                break;
            }
        }
        return defaultHtmlFile;
    }
    
    public static String joinPath(final String lhs, final String rhs)
    {
        return lhs.endsWith(File.separator) ?
                (rhs.startsWith(File.separator) ? lhs + rhs.substring(1) : lhs + rhs) :
                (rhs.startsWith(File.separator) ? lhs + rhs : lhs + File.separator + rhs);
    }    
    
    public String resolveContentType(final File document) throws IOException
    {
        String contentType = Files.probeContentType(document.toPath());
        if (null == contentType)
        {
            int lastPeriod = document.getName().lastIndexOf(".");
            if (-1 != lastPeriod)
            {
                final String ext = document.getName().substring(lastPeriod);
                if (".html".equalsIgnoreCase(ext) || ".htm".equalsIgnoreCase(ext))
                {
                    contentType = FilesystemResponseStrategy.CONTENT_TYPE_TEXT_HTML;
                }
                else if (".txt".equalsIgnoreCase(ext))
                {
                    contentType = FilesystemResponseStrategy.CONTENT_TYPE_TEXT_PLAIN;
                }
            }
        }
        return null == contentType ? FilesystemResponseStrategy.CONTENT_TYPE_DEFAULT : contentType;
    }    
}
