package org.mvryan.http.response.filesys;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;
import javax.inject.Named;

import lombok.extern.slf4j.Slf4j;

import org.mvryan.http.modules.ConfigurationModule;
import org.mvryan.http.request.HttpRequest;
import org.mvryan.http.response.HttpResponse;
import org.mvryan.http.response.HttpResponseCode;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

@Slf4j
public class FilesystemResponseStrategy implements HttpResponseStrategy
{
    private final FilesystemResolver fileResolver;
    private final String documentRoot;
    private final boolean allowDirectoryIndex;
    
    private final Optional<LoadingCache<HttpRequest, HttpResponse>> cache;
    
    @Inject
    public FilesystemResponseStrategy(@Named(ConfigurationModule.DOCUMENT_ROOT) final String documentRoot,
            @Named(ConfigurationModule.ALLOW_DIRECTORY_INDEX) final String allowDirectoryIndex,
            @Named(ConfigurationModule.CACHE_ENABLED) final String cacheEnabled,
            final FilesystemResolver fileResolver)
    {
        this.documentRoot = documentRoot;
        this.allowDirectoryIndex = Boolean.parseBoolean(allowDirectoryIndex);
        this.fileResolver = fileResolver;
        
        if (Boolean.parseBoolean(cacheEnabled))
        {
            cache = Optional.of(CacheBuilder.newBuilder()
                    .build(
                            new CacheLoader<HttpRequest, HttpResponse>()
                            {
                                public HttpResponse load(final HttpRequest request)
                                {
                                    return processRequest(request);
                                }
                            }));
        }
        else
        {
            cache = Optional.empty();
        }
    }

    @Override
    public HttpResponse determineResponse(final HttpRequest request)
    {
        log.debug(String.format("Determining response for requested path \"%s\"", request.getUri().getPath()));
        
        if (cache.isPresent())
        {
            try
            {
                return cache.get().get(request);
            }
            catch (ExecutionException e)
            {
                return processRequest(request);
            }
        }
        else
        {
            return processRequest(request);
        }
    }
    
    private HttpResponse processRequest(final HttpRequest request)
    {
        File documentRootDir = fileResolver.getFile(documentRoot);
        if (documentRootDir.exists() && documentRootDir.isDirectory())
        {
            File document = fileResolver.getFile(FilesystemResolver.joinPath(documentRoot, request.getUri().getPath()));
            if (document.exists())
            {
                if (document.canRead())
                {
                    return processDocument(document, request);
                }
                else
                {
                    return FilesystemHttpResponse.builder().responseCode(HttpResponseCode.FORBIDDEN).build();
                }
            }
            else
            {
                return FilesystemHttpResponse.builder().responseCode(HttpResponseCode.FILE_NOT_FOUND).build();
            }
        }
        else
        {
            log.error("Document root does not exist or is not directory: " + documentRoot);
            return FilesystemHttpResponse.builder().responseCode(HttpResponseCode.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    private HttpResponse processDocument(final File document, final HttpRequest request)
    {
        if (document.isDirectory())
        {
            final File defaultHtmlFile = fileResolver.getDefaultHtmlFile(document);
            if (null == defaultHtmlFile)
            {
                // No default found, do an index
                log.debug(String.format("Trying to perform index render on path \"%s\"", document.getAbsolutePath()));
                return processDirectoryIndex(document);
            }
            else
            {
                log.debug(String.format("Found default HTML file \"%s\" for requested path \"%s\"", defaultHtmlFile.getName(), document.getAbsolutePath()));
                return processDocument(defaultHtmlFile, request);
            }
        }
        else
        {
            HttpResponseCode responseCode = HttpResponseCode.INTERNAL_SERVER_ERROR;
            byte[] responsePayload = null;
            String contentType = HttpResponseStrategy.CONTENT_TYPE_DEFAULT;
            
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
                    log.debug(String.format("Found file \"%s\"", document.getAbsolutePath()));
                    
                    contentType = fileResolver.resolveContentType(document);
                    log.debug(String.format("Resolved response content type: \"%s\"", contentType));
                    
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
                    // this seems like a system error at this point
                    log.error("Couldn't serve file " + document.getAbsolutePath() + ": ", e);
                    responseCode = HttpResponseCode.INTERNAL_SERVER_ERROR;
                }
            }
            
            return FilesystemHttpResponse.builder()
                    .responseCode(responseCode)
                    .responsePayload(responsePayload)
                    .contentType(contentType)
                    .build();
        }
    }
    
    private HttpResponse processDirectoryIndex(final File directory)
    {
        if (allowDirectoryIndex)
        {
            final StringBuilder sb = new StringBuilder();
            final String dirPath = getRelativePath(directory.getAbsolutePath());
            sb.append(String.format("<html><head><title>Index of %s</title></head>\n", dirPath));
            sb.append(String.format("<body>%s<hr/>\n<a href=\"%s\">.</a><br/>\n", dirPath, dirPath));
            if (! dirPath.equals("/"))
            {
                String parentDirPath = dirPath.substring(0, dirPath.lastIndexOf(File.separator, dirPath.length()-2));
                if (0 == parentDirPath.length())
                {
                    parentDirPath = "/";
                }
                sb.append(String.format("<a href=\"%s\">..</a><br/>\n", parentDirPath));
            }
            for (final File file : directory.listFiles())
            {
                sb.append(String.format("<a href=\"%s\">%s</a><br/>\n", getRelativePath(file.getAbsolutePath()), file.getName()));
            }
            sb.append("</body></html>\n");
            
            return FilesystemHttpResponse.builder()
                    .responseCode(HttpResponseCode.OK)
                    .responsePayload(sb.toString().getBytes())
                    .contentType(CONTENT_TYPE_TEXT_HTML)
                    .build();
        }
        else
        {
            log.debug("Attempted unsupported directory index");
            
            return FilesystemHttpResponse.builder()
                    .responseCode(HttpResponseCode.FORBIDDEN)
                    .responsePayload("Directory index not supported".getBytes())
                    .build();
        }
    }
    
    private String getRelativePath(final String absolutePath)
    {
        String relativePath = absolutePath;
        if (absolutePath.startsWith(documentRoot))
        {
            relativePath = absolutePath.substring(documentRoot.endsWith(File.separator) ? documentRoot.length()-1 : documentRoot.length());
        }
        if (relativePath.endsWith("/"))
        {
            relativePath = relativePath.substring(0, relativePath.length()-1);
        }
        return relativePath;
    }
    
    private boolean contentTypeMatchesAccept(final String contentType, final HttpRequest request)
    {
        final String acceptHeader = request.getHeaders().get("Accept");
        if (null == acceptHeader)
        {
            return true;
        }
        
        for (final String ct : acceptHeader.split(","))
        {
            final String thisCt = ct.split(";",2)[0].trim();
            if ("*/*".equals(thisCt))
            {
                return true;
            }
            else if (contentType.equalsIgnoreCase(thisCt))
            {
                return true;
            }
        }
        return false;
    }
}
