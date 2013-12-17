package com.mvas.webproxy.config;

import com.mvas.webproxy.DeviceConnectionInfo;
import com.mvas.webproxy.RequestData;
import com.mvas.webproxy.portals.AbstractRequestHandler;
import com.mvas.webproxy.portals.AbstractServerChecker;
import com.mvas.webproxy.portals.StalkerServerChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;


public class PortalConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(PortalConfiguration.class);
    static Config config;

    // Name, Checker
    private static final ArrayList<AbstractServerChecker> requestHandlersList = new ArrayList<>();

    private static final String CONFIG_PORTAL = "portal";
    private static final String COOKIE = "cookie";

    static {
        requestHandlersList.add(new StalkerServerChecker());
    }

    private static volatile PortalConfiguration instance;
    public static PortalConfiguration getInstance() {
        PortalConfiguration localInstance = instance;
        if (localInstance == null) {
            synchronized (PortalConfiguration.class) {
                localInstance = instance;
                if (localInstance == null) {
                    //logger.debug("Creating instance of PortalConfiguration");
                    instance = localInstance = new PortalConfiguration();
                    config = Config.getInstance(new IniConfig());
                    config.init();
                }
            }
        }
        return localInstance;
    }

    public static ArrayList<AbstractServerChecker> getRequestHandlers()
    {
        return requestHandlersList;
    }

    public synchronized  AbstractRequestHandler getHandler(final DeviceConnectionInfo connectionInfo, final RequestData requestData) throws MalformedURLException {
        if(connectionInfo.getRequestHandler() == null)
        {
            //Get handler from config
            String portalName = config.get(requestData.getConnectionName(), CONFIG_PORTAL);
            for(AbstractServerChecker checker: requestHandlersList)
            {
                if(checker.getName().equals(portalName))
                {
                    connectionInfo.setRequestHandler(checker.getHandler(connectionInfo));
                    break;
                }
            }

            //Trying to guess handler from portal signatures
            for(AbstractServerChecker checker: getRequestHandlers())
            {
                AbstractRequestHandler handler = checker.check(connectionInfo, requestData) ;
                if(handler != null)
                {
                    connectionInfo.setRequestHandler(handler);
                    break;
                }
            }
        }
        return connectionInfo.getRequestHandler();
    }

    public synchronized String get(final String group, final String name)
    {
        return config.get(group, name);
    }

    public synchronized String set(final String group, final String name, final String value)
    {
        return config.set(group, name, value);
    }

    public synchronized ArrayList<String> getGroups()
    {
        return config.getGroups();
    }

    public void loadConfiguration(final RequestData requestData) throws MalformedURLException
    {
        if(requestData.getRealUrl() == null)
        {
            String str = get(requestData.getConnectionName(), "url");
            if(str != null)
                requestData.setRealUrl(new URL( str));
        }

        if(requestData.getMacAddress() == null)
        {
            requestData.setMacAddress(get(requestData.getConnectionName(), "mac"));
        }
    }
}
