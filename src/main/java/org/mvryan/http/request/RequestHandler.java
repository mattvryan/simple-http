package org.mvryan.http.request;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.mvryan.http.response.HttpResponse;
import org.mvryan.http.response.HttpResponseCode;
import org.mvryan.http.response.HttpResponseFactory;

import com.google.inject.Injector;

@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level=AccessLevel.PRIVATE)
public class RequestHandler implements Runnable
{
    final Socket socket;
    final Injector injector;
    
    Timer keepaliveTimer = null;
    Timer maxKeepaliveTimer = null;
    
    private static final int KEEPALIVE_TIMEOUT_SECONDS = 15;
    private static final int MAX_KEEPALIVE_TIMEOUT_SECONDS = 100;
    
    @Override
    public void run()
    {
        final HttpRequest request = injector.getInstance(HttpRequest.class);
        try
        {
            while(true)
            {
                HttpResponseCode responseCode = request.parse(socket.getInputStream());
                
                log.info(String.format("Request: %s %s", request.getMethod(), request.getUri().getPath()));            
                
                if (responseCode.isError() || responseCode.isRedirect())
                {
                    respondAndClose(request, responseCode);
                    break;
                }
                else
                {
                    final HttpResponseFactory factory = injector.getInstance(HttpResponseFactory.class);
                    final HttpResponse response = factory.getResponse(request);
                    responseCode = response.getResponseCode();
                    
                    if (responseCode.isError())
                    {
                        respondAndClose(request, responseCode, Optional.of(response));
                        break;
                    }
                    else
                    {
                        if (request.isKeepalive())
                        {
                            manageKeepalives();
                            respond(request, responseCode, Optional.of(response));
                        }
                        else
                        {
                            respondAndClose(request, responseCode, Optional.of(response));
                            break;
                        }
                    }
                }
            }
        }
        catch (SocketException se)
        {
            // This happens when we close the socket while trying to read,
            // which happens if a keepalive timer expires.  Nothing to
            // worry about here.
        }
        catch (Exception e)
        {
            log.error("Exception caught handling request", e);
            try
            {
                respondAndClose(request, HttpResponseCode.INTERNAL_SERVER_ERROR);
            }
            catch (IOException ioe)
            {
                // This is bad - all we can really do is log the error
                log.error("Unable to send client response", ioe);
            }
        }
    }
    
    private void respond(final HttpRequest request,
            final HttpResponseCode responseCode,
            final Optional<HttpResponse> response)
            throws IOException
    {
        synchronized(this)
        {
            final PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            writer.println(String.format("HTTP/1.1 %d %s", responseCode.getStatus(), responseCode.getReason()));
            writer.println(String.format("Date: %s", DateTime.now().toString(ISODateTimeFormat.dateTime())));
            writer.println("Server: Simple HTTP server version 0.0.1 (org.mvryan.http)");
            if (request.isKeepalive())
            {
                writer.println("Connection: keep-alive");
                writer.println(String.format("Keep-Alive: timeout=%d, max=%d", KEEPALIVE_TIMEOUT_SECONDS, MAX_KEEPALIVE_TIMEOUT_SECONDS));
            }
            else
            {
                writer.println("Connection: close");
            }
            if (response.isPresent())
            {
                HttpResponse rsp = response.get();
                if (null != rsp.getResponsePayload())
                {
                    writer.println(String.format("Content-Length: %d", rsp.getResponsePayload().length));
                    writer.println(String.format("Content-Type: %s", rsp.getContentType()));
                    writer.println("");
                    socket.getOutputStream().write(rsp.getResponsePayload());
                }
            }
        }
        
        if (responseCode.isError())
        {
            log.error(String.format("Response (ERROR): %d %s (Request: %s %s)",
                    responseCode.getStatus(),
                    responseCode.getReason(),
                    request == null ? "?" : request.getMethod(),
                    request == null ? "?" : request.getUri().getPath()));
        }
        else
        {
            log.info(String.format("Response: %d %s (Request: %s %s)",
                    responseCode.getStatus(),
                    responseCode.getReason(),
                    request == null ? "?" : request.getMethod(),
                    request == null ? "?" : request.getUri().getPath()));
        }
    }
    
    private void respondAndClose(final HttpRequest request, final HttpResponseCode responseCode) throws IOException
    {
        respondAndClose(request, responseCode, Optional.empty());
    }
    
    private void respondAndClose(final HttpRequest request, final HttpResponseCode responseCode, 
            final Optional<HttpResponse> response)
        throws IOException
    {
        respond(request, responseCode, response);
        closeSocket();
    }
    
    private void manageKeepalives()
    {
        if (null != keepaliveTimer)
        {
            keepaliveTimer.cancel();
            keepaliveTimer = null;
        }
        
        keepaliveTimer = new Timer();
        keepaliveTimer.schedule(new TimerTask()
        {
            @Override public void run()
            {
                try
                {
                    maxKeepaliveTimer.cancel();
                    maxKeepaliveTimer = null;
                    closeSocket();
                    log.debug("Keepalive timeout expired - socket closed");
                    keepaliveTimer.cancel();
                    keepaliveTimer = null;
                }
                catch (IOException e)
                {
                    log.debug("Unable to close socket", e);
                }
            }
        }, KEEPALIVE_TIMEOUT_SECONDS*1000);
        
        if (null == maxKeepaliveTimer)
        {
            maxKeepaliveTimer = new Timer();
            maxKeepaliveTimer.schedule(new TimerTask()
            {
                @Override public void run()
                {
                    try
                    {
                        keepaliveTimer.cancel();
                        keepaliveTimer = null;
                        closeSocket();
                        log.debug("Max keepalive timeout expired - socket closed");
                        maxKeepaliveTimer.cancel();
                        maxKeepaliveTimer = null;
                    }
                    catch (IOException e)
                    {
                        log.debug("Unable to close socket", e);
                    }
                }
            }, MAX_KEEPALIVE_TIMEOUT_SECONDS*1000);
        }        
    }
    
    private synchronized void closeSocket() throws IOException
    {
        socket.close();
    }
}
