package org.mvryan.http.request;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import org.mvryan.http.response.HttpResponseCode;

@Slf4j
@Value
public class RequestHandler implements Runnable
{
    Socket socket;

    @Override
    public void run()
    {
        try
        {
            final HttpRequest request = new HttpRequest();
            HttpResponseCode responseCode = request.parse(socket.getInputStream());
            
            final PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            writer.println(String.format("HTTP/1.1 %d %s",
                    responseCode.getStatus(),
                    responseCode.getReason()));
            socket.close();
        }
        catch (IOException e)
        {
            
        }
    }
}
