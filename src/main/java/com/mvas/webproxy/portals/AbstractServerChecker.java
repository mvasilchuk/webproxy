package com.mvas.webproxy.portals;


import com.mvas.webproxy.DeviceConnectionInfo;
import com.mvas.webproxy.RequestData;

import java.net.MalformedURLException;

public abstract class AbstractServerChecker {

    public abstract AbstractRequestHandler check(DeviceConnectionInfo connInfo, RequestData requestData) throws MalformedURLException;
    public abstract String getName();
    public abstract AbstractRequestHandler getHandler(final DeviceConnectionInfo connInfo);
}
