package org.mvryan.http.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.mvryan.http.modules.HttpServerModule;
import org.mvryan.http.request.RequestHandler;

import com.google.inject.Guice;
import com.google.inject.Injector;

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
            Injector injector = Guice.createInjector(new HttpServerModule());
            serverSocket = new ServerSocket(port);
            while (keep_running)
            {
                pool.execute(new RequestHandler(serverSocket.accept(), injector));
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
            catch (IOException e) { }
        }
        
        pool.shutdown();
        try
        {
            pool.awaitTermination(500, TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException e) { }
        if (! pool.isTerminated())
        {
            pool.shutdownNow();
        }
    }
}
