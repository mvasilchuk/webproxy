package com.mvas.webproxy;

import com.mvas.webproxy.portals.AbstractRequestHandler;

import java.net.URLConnection;
import java.util.HashMap;

public class DeviceConnectionInfo {
    String name;
    URLConnection connection;

    public AbstractRequestHandler getRequestHandler() {
        return requestHandler;
    }

    public void setRequestHandler(AbstractRequestHandler requestHandler) {
        this.requestHandler = requestHandler;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setConnection(URLConnection connection) {
        this.connection = connection;
    }

    AbstractRequestHandler requestHandler;

    HashMap<String, String> staticParams = new HashMap<>(); //Params that should be the same for different devices

    public HashMap<String, String> getStaticParams() {
        return staticParams;
    }


    public URLConnection getConnection() {
        return connection;
    }

    public String getName() {
        return name;
    }


}
