package org.mvryan.http;

import lombok.extern.slf4j.Slf4j;

import org.mvryan.http.server.HttpServer;
import org.mvryan.http.server.Server;

@Slf4j
public class Main
{
    public static void main(String[] args)
    {
        final Server server = new HttpServer();
        
        Runtime.getRuntime().addShutdownHook(new Thread(){
            @Override public void run()
            {
                server.stop();
                log.info("Server stopped.");
            }
        });
        
        log.info("Server starting.");
        server.start(1234);
    }
}
