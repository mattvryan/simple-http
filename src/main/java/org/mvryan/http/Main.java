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
        server.start(1234);
        log.info("Server started.");
        
        server.stop();
        log.info("Server stopped.");
    }
}
