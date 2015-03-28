package org.mvryan.http.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;

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
            final BufferedReader reader =
                    new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String line;
            while (null != (line = reader.readLine()))
            {
                if (line.length() < 1) break;
                log.debug(line);
            }
            
            final PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            writer.println("HTTP/1.1 200 OK");
            socket.close();
        }
        catch (IOException e)
        {
            
        }
    }
}
