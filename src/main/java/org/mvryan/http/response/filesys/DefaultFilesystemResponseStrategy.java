package org.mvryan.http.response.filesys;

import java.io.File;
import java.io.IOException;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.mvryan.http.request.HttpRequest;
import org.mvryan.http.response.HttpResponseCode;

@Slf4j
public class DefaultFilesystemResponseStrategy implements FilesystemResponseStrategy
{
    @Getter
    private HttpResponseCode responseCode = HttpResponseCode.OK;
    @Getter
    private byte[] responsePayload = null;
    @Getter
    private String contentType = CONTENT_TYPE_DEFAULT; // HTTP default
    
    
    private final FilesystemResolver fileResolver;
    
    public DefaultFilesystemResponseStrategy()
    {
        this(new FilesystemResolver());
    }
    
    public DefaultFilesystemResponseStrategy(final FilesystemResolver fileResolver)
    {
        this.fileResolver = fileResolver;
    }

    @Override
    public void determineResponse(String documentRootPath, HttpRequest request)
    {
        File documentRootDir = fileResolver.getFile(documentRootPath);
        if (documentRootDir.exists() && documentRootDir.isDirectory())
        {
            File document = fileResolver.getFile(FilesystemResolver.joinPath(documentRootPath, request.getUri().getPath()));
            if (document.exists())
            {
                if (document.canRead())
                {
                    processDocument(document, request);
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
    }
    
    private void processDocument(final File document, final HttpRequest request)
    {
        if (document.isDirectory())
        {
            final File defaultHtmlFile = fileResolver.getDefaultHtmlFile(document);
            if (null == defaultHtmlFile)
            {
                // No default found, do an index
                processDirectoryIndex(document);
            }
            else
            {
                processDocument(defaultHtmlFile, request);
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
                    contentType = fileResolver.resolveContentType(document);
                    if (! contentTypeMatchesAccept(contentType, request))
                    {
                        responsePayload = contentType.getBytes();
                        responseCode = HttpResponseCode.NOT_ACCEPTABLE;
                    }
                    else
                    {
                        responsePayload = fileResolver.readFileBytes(document);
                        responseCode = HttpResponseCode.OK;
                    }
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
    
    private void processDirectoryIndex(final File directory)
    {
        responsePayload = "Directory index not supported".getBytes();
        responseCode = HttpResponseCode.FORBIDDEN;
    }
    
    private boolean contentTypeMatchesAccept(final String contentType, final HttpRequest request)
    {
        final String acceptHeader = request.getHeaders().get("Accept");
        if (null == acceptHeader || acceptHeader.equals("*/*"))
        {
            return true;
        }
        
        for (final String ct : acceptHeader.split(","))
        {
            if (contentType.equalsIgnoreCase(ct.split(";", 2)[0].trim()))
            {
                return true;
            }
        }
        return false;
    }
}
