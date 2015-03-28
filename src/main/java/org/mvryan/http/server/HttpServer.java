package org.mvryan.http.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpServer implements Server
{
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private boolean keep_running = true;
    private ServerSocket serverSocket;
    
    @Override
    public void start(int port)
    {
        try
        {
            serverSocket = new ServerSocket(port);
            while (keep_running)
            {
                pool.execute(new RequestHandler(serverSocket.accept()));
            }
            stop();
        }
        catch (IOException e)
        {
            // TODO
        }

    }

    @Override
    public void stop()
    {
        keep_running = false;
        if (! serverSocket.isClosed())
        {
            try
            {
                serverSocket.close();
            }
            catch (IOException e)
            {
                
            }
        }
    }
}
