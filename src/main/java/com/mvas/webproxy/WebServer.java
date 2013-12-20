package com.mvas.webproxy;

import com.mvas.webproxy.config.PortalConfiguration;
import com.mvas.webproxy.exceptions.ErrorCodes;
import com.mvas.webproxy.exceptions.WebServerException;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;


/**
 * @author MaximVasilchuk <mvasilchuk@gmail.com>
 */

public class WebServer  {
    private static final Logger logger = LoggerFactory.getLogger(WebServer.class);

    private static final String BIND_ADDRESS = "0.0.0.0";
    private static final int BIND_PORT = 8088;

    public static final String WEB_ROOT = "/webdir/";
    public static final String INDEX_FILE = "index.html";
    public static final String WEB_ROOT_DIR = "/";

    private Server server = null;
    private static volatile WebServer instance;
    HandlerCollection handlerCollection;

    boolean runningOnEmulator = false;

    static final class WebServerInstanceInfo {
        String bindAddress;
        Integer port;
    }

    private static final ArrayList<WebServerInstanceInfo> serverInstances = new ArrayList<>();

    public static PortalConfiguration getPortalConfiguration() {
        return portalConfiguration;
    }

    static PortalConfiguration portalConfiguration;



    public void runWebServer() throws WebServerException
    {
        listLocalIpAddresses();

        final WebServer server = WebServer.getInstance();

        ArrayList<String> configGroups = portalConfiguration.getGroups();

        for (String configGroup : configGroups) {
            WebServerInstanceInfo instance = new WebServerInstanceInfo();

            String[] address = configGroup.split(":");
            if (address.length == 2)
            {
                instance.bindAddress = address[0];
                instance.port = Integer.parseInt(address[1]);
            }
            else if (address.length == 1) {
                instance.bindAddress = address[0];
                instance.port = 80;
            }

            serverInstances.add(instance);
        }

        this.server = server.startServer();


        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run()
            {
                server.stopServers();
            }
        });
    }

    public static WebServer getInstance(final PortalConfiguration portalConfiguration, final HashMap<String, String> arguments)
    {
        WebServer localInstance = instance;
        if (localInstance == null) {
            synchronized (WebServer.class) {
                localInstance = instance;
                if (localInstance == null) {
                    if(portalConfiguration == null)
                        throw new IllegalStateException("Configuration not found!");
                    if(arguments == null)
                        throw new IllegalStateException("Command line arguments not found!");

                    WebServer.portalConfiguration = portalConfiguration;
                    instance = localInstance = new WebServer();
                }
            }
        }
        return localInstance;
    }

    public static WebServer getInstance() {
        return getInstance(null, null);
    }

    public static String listLocalIpAddresses(){
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
                 en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress.isSiteLocalAddress()) {
                        logger.debug("Local address: " + inetAddress.getHostAddress());
                    }
                }
            }
        } catch (Exception ex) {
            logger.error(ex.toString());
        }
        return null;
    }

    protected Server startServer() throws WebServerException
    {
        Server server = new Server();

        int instancesSize = serverInstances.size();
        Connector[] ports = new Connector[instancesSize];
        for(int index = 0; index < instancesSize; index++)
        {
            Connector connector = new SelectChannelConnector();
            connector.setHost(BIND_ADDRESS);
            connector.setPort(serverInstances.get(index).port);
            ports[index] = connector;
        }
        server.setConnectors(ports);

        handlerCollection = new HandlerCollection();
        initMainWebHandler(server);
        server.setHandler(handlerCollection);

        try {
            server.start();
        } catch (Exception e) {
            logger.error(e.toString());
            throw new WebServerException(ErrorCodes.E_WEB_SERVER_ERROR);
        }
        try {
            server.join();
        } catch (InterruptedException e) {
            logger.error(e.toString());
            throw new WebServerException(ErrorCodes.E_WEB_SERVER_ERROR);
        }


        return server;
    }

    public void initMainWebHandler(final Server server)
    {
        String webDir = System.class.getResource(WEB_ROOT).toExternalForm();
        ContextHandler rootContext = new ContextHandler(server, WEB_ROOT_DIR);
        rootContext.setResourceBase(webDir);
        rootContext.setHandler(new DefaultHandler(rootContext));
        handlerCollection.addHandler(rootContext);
    }

    public void stopServers()
    {
        logger.debug("Stopping web server...");
        if(server != null)
            try {
                server.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
    }
}
