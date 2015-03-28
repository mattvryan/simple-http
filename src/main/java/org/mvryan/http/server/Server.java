package org.mvryan.http.server;

public interface Server
{
    void start(int port);
    void stop();
}
