package com.mvas.webproxy;

import com.mvas.webproxy.config.PortalConfiguration;
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

    private HashMap<Integer, Server> servers = new HashMap<>();
    private static volatile WebServer instance;
    HandlerCollection handlerCollection;

    boolean runningOnEmulator = false;

    static final class WebServerInstanceInfo {
        String bindAddress;
        Integer port;
        Server server;
        Thread thread;
    }

    private static final ArrayList<WebServerInstanceInfo> serverInstances = new ArrayList<>();

    public static PortalConfiguration getPortalConfiguration() {
        return portalConfiguration;
    }

    static final PortalConfiguration portalConfiguration;

    static {
        portalConfiguration = PortalConfiguration.getInstance();
    }


    public static void main(String[] args)
    {

        runWebServer(args);
    }

    public static void runWebServer(final String[] args)
    {
        listLocalIpAddresses();

        final WebServer server = WebServer.getInstance();

        ArrayList<String> configGroups = portalConfiguration.getGroups();


        for (String configGroup : configGroups) {
            WebServerInstanceInfo instance = new WebServerInstanceInfo();

            String[] address = configGroup.split(":");
            if (address.length == 2)
            {
                //instance.bindAddress = address[0];
                instance.port = Integer.parseInt(address[1]);
            }
            else if (address.length == 1) {
                //instance.bindAddress = address[0];
                instance.port = 80;
            }

            serverInstances.add(instance);
        }
        server.startServer();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run()
            {
                server.stopServers();
            }
        });
    }

    public static WebServer getInstance() {
        WebServer localInstance = instance;
        if (localInstance == null) {
            synchronized (WebServer.class) {
                localInstance = instance;
                if (localInstance == null) {
                    instance = localInstance = new WebServer();
                }
            }
        }
        return localInstance;
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

    protected Server startServer()
    {
        Server server = new Server();

        int instancesSize = serverInstances.size();
        Connector[] ports = new Connector[instancesSize];
        for(int index = 0; index < instancesSize; index++)
        {
            Connector connector = new SelectChannelConnector();
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
            e.printStackTrace();
        }
        try {
            server.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
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
        logger.debug("Stopping web servers...");
        for(WebServerInstanceInfo instance: serverInstances)
        {
            try {
                if(instance != null && instance.server != null)
                    instance.server.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
