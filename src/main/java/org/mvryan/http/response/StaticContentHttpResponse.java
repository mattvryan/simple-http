package org.mvryan.http.response;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.mvryan.http.modules.ConfigurationModule;
import org.mvryan.http.request.HttpRequest;

import com.google.common.collect.Lists;

@Slf4j
public class StaticContentHttpResponse implements HttpResponse
{
    @Getter
    private HttpResponseCode responseCode;
    @Getter
    private byte[] responsePayload = null;
    
    private final String documentRootPath;
    
    private final List<String> defaultHtmlDocs =
            Lists.newArrayList("index.html", "index.htm", "default.html", "default.htm");
    
    @Inject
    public StaticContentHttpResponse(@Named(ConfigurationModule.DOCUMENT_ROOT) final String documentRoot)
    {
        responseCode = HttpResponseCode.OK;
        documentRootPath = documentRoot;
    }
    
    @Override
    public StaticContentHttpResponse forRequest(final HttpRequest request)
    {
        File documentRootDir = new File(documentRootPath);
        if (documentRootDir.exists() && documentRootDir.isDirectory())
        {
            File document = new File(joinPath(documentRootPath, request.getUri().getPath()));
            if (document.exists())
            {
                if (document.canRead())
                {
                    processDocument(document);
                }
                else
                {
                    responseCode = HttpResponseCode.FORBIDDEN;
                }
            }
            else
            {
                responseCode = HttpResponseCode.FILE_NOT_FOUND;
            }
        }
        else
        {
            log.error("Document root does not exist or is not directory: " + documentRootPath);
            responseCode = HttpResponseCode.INTERNAL_SERVER_ERROR;
        }
        return this;
    }
    
    private String joinPath(final String lhs, final String rhs)
    {
        return lhs.endsWith(File.separator) ? lhs + rhs : lhs + File.separator + rhs;
    }
    
    private void processDocument(final File document)
    {
        if (document.isDirectory())
        {
            final File defaultHtmlFile = getDefaultHtmlFile(document);
            if (null == defaultHtmlFile)
            {
                // No default found, do an index
                processDirectoryIndex(document);
            }
            else
            {
                processDocument(defaultHtmlFile);
            }
        }
        else
        {
            // Double-check file existence and access
            if (! document.exists())
            {
                responseCode = HttpResponseCode.FILE_NOT_FOUND;
            }
            else if (! document.canRead())
            {
                responseCode = HttpResponseCode.FORBIDDEN;
            }
            else
            {
                try
                {
                    responsePayload = Files.readAllBytes(document.toPath());
                    responseCode = HttpResponseCode.OK;
                }
                catch (IOException e)
                {
                    // We already checked file existence and access;
                    // this seems like a system error
                    log.error("Couldn't serve file " + document.getAbsolutePath() + ": ", e);
                    responseCode = HttpResponseCode.INTERNAL_SERVER_ERROR;
                }
            }
        }
    }
    
    private File getDefaultHtmlFile(final File directory)
    {
        File defaultHtmlFile = null;
        for (final String defaultHtml : defaultHtmlDocs)
        {
            defaultHtmlFile = new File(joinPath(directory.getAbsolutePath(), defaultHtml));
            if (defaultHtmlFile.exists() && defaultHtmlFile.isFile())
            {
                break;
            }
        }
        return defaultHtmlFile;
    }
    
    private void processDirectoryIndex(final File directory)
    {
        responsePayload = "Directory index not supported".getBytes();
        responseCode = HttpResponseCode.FORBIDDEN;
    }
}
