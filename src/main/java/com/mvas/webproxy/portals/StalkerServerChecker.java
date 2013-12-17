package com.mvas.webproxy.portals;

import com.mvas.webproxy.DeviceConnectionInfo;
import com.mvas.webproxy.RequestData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;


public class StalkerServerChecker extends AbstractServerChecker {
    private static final Logger logger = LoggerFactory.getLogger(StalkerServerChecker.class);
    public static final String HANDLER_NAME = "stalker";

    @Override
    public AbstractRequestHandler check(DeviceConnectionInfo connInfo, RequestData requestData) throws MalformedURLException {
        final URL portalRealUrl = requestData.getRealUrl();

        if(portalRealUrl == null)
            throw new MalformedURLException();

        logger.debug("connInfo.requestData.target: " + portalRealUrl);
        if(portalRealUrl.getPath().contains("stalker_portal"))
        {
            logger.debug("Stalker portal found!");
            return getHandler(connInfo);
        }
        return null;
    }

    @Override
    public String getName() {
        return HANDLER_NAME;
    }

    @Override
    public AbstractRequestHandler getHandler(DeviceConnectionInfo connInfo)
    {
        return new StalkerRequestHandler(connInfo);
    }
}
