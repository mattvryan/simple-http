package org.mvryan.http.request;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Optional;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.mvryan.http.response.HttpResponse;
import org.mvryan.http.response.HttpResponseCode;
import org.mvryan.http.response.HttpResponseFactory;

import com.google.inject.Injector;

@Slf4j
@Value
public class RequestHandler implements Runnable
{
    Socket socket;
    Injector injector;

    @Override
    public void run()
    {
        try
        {
            final HttpRequest request = injector.getInstance(HttpRequest.class);
            HttpResponseCode responseCode = request.parse(socket.getInputStream());
            
            if (responseCode.isError() || responseCode.isRedirect())
            {
                respondAndClose(responseCode);
            }
            else
            {
                final HttpResponseFactory factory = injector.getInstance(HttpResponseFactory.class);
                final HttpResponse response = factory.getResponse(request);
                responseCode = response.getResponseCode();
                
                if (responseCode.isError())
                {
                    respondAndClose(responseCode, Optional.of(response));
                }
                else
                {
                    if (request.isKeepalive())
                    {
                        respond(responseCode, Optional.of(response));
                    }
                    else
                    {
                        respondAndClose(responseCode, Optional.of(response));
                    }
                }
            }
        }
        catch (Exception e)
        {
            log.error("Exception caught handling request", e);
            try
            {
                respondAndClose(HttpResponseCode.INTERNAL_SERVER_ERROR);
            }
            catch (IOException ioe)
            {
                // This is bad - all we can really do is log the error
                log.error("Unable to send client response", ioe);
            }
        }
    }
    
    private void respond(final HttpResponseCode responseCode,
            final Optional<HttpResponse> response)
            throws IOException
    {
        final PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
        writer.println(String.format("HTTP/1.1 %d %s", responseCode.getStatus(), responseCode.getReason()));
        writer.println(String.format("Date: %s", DateTime.now().toString(ISODateTimeFormat.dateTime())));
        writer.println("Server: Simple HTTP server version 0.0.1 (org.mvryan.http)");
        writer.println("Connection: close");
        if (response.isPresent())
        {
            HttpResponse rsp = response.get();
            if (null != rsp.getResponsePayload())
            {
                writer.println(String.format("Content-Length: %d", rsp.getResponsePayload().length));
//                // headers - Content-Type
                writer.println("");
                socket.getOutputStream().write(rsp.getResponsePayload());
            }
        }
    }
    
    private void respondAndClose(final HttpResponseCode responseCode) throws IOException
    {
        respondAndClose(responseCode, Optional.empty());
    }
    
    private void respondAndClose(final HttpResponseCode responseCode, 
            final Optional<HttpResponse> response)
        throws IOException
    {
        respond(responseCode, response);
        socket.close();
    }
}
