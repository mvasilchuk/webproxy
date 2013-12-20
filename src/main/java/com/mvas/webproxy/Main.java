package com.mvas.webproxy;


import com.mvas.webproxy.config.IniConfig;
import com.mvas.webproxy.config.PortalConfiguration;
import com.mvas.webproxy.exceptions.ErrorCodes;
import com.mvas.webproxy.exceptions.WebServerException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(WebServer.class);

    public static final String APP_VERSION = "1.0";
    public static final String APP_DESCRIPTION = "WebProxy for STB Emulator";
    public static final String APP_VERSION_STRING = "version " + APP_VERSION;
    public static final String APP_COPYRIGHT = "Copyright(c) 2013 by Maxim Vasilchuk";

    private static final int LINE_WIDTH = 80;
    private static final int INNER_WIDTH = LINE_WIDTH - 2;



    public static void main(String[] args)
    {
        //BasicConfigurator.configure();

        HashMap<String, String> arguments;
        try {
            arguments = ArgumentParser.getArguments(args);
        }
        catch (IllegalArgumentException e)
        {
            logger.error(e.toString());
            System.exit(1);
            return;
        }

        if(arguments.containsKey("help") || arguments.containsKey("h"))
        {
            printHelp();
            System.exit(ErrorCodes.E_NO_ERROR);
        }

        PortalConfiguration portalConfiguration;
        try {
            portalConfiguration = PortalConfiguration.getInstance(new IniConfig(arguments));
        } catch (IOException e) {
            logger.error(e.toString());
            System.exit(ErrorCodes.E_CONFIG_ACCESS_ERROR);
            return;
        }

        ArrayList<String> configGroups = portalConfiguration.getGroups();

        if(configGroups.size() == 0)
        {
            logger.warn("No portal configuration found! Check if you have at least one configuration in your configuration file");
            printConfigExample();
            System.exit(ErrorCodes.E_NO_ERROR);
            return;
        }

        printHeader();
        try {
            WebServer.getInstance(portalConfiguration, arguments).runWebServer();
        }
        catch (WebServerException e)
        {
            System.exit(e.getCode());
        }

    }

    private static void printHeader()
    {
        System.out.println(StringUtils.repeat("=", LINE_WIDTH));
        System.out.println("=" + StringUtils.center(APP_DESCRIPTION,    INNER_WIDTH) + "=");
        System.out.println("=" + StringUtils.center(APP_VERSION_STRING, INNER_WIDTH) + "=");
        System.out.println("=" + StringUtils.center(APP_COPYRIGHT,      INNER_WIDTH) + "=");
        System.out.println(StringUtils.repeat("=", LINE_WIDTH));
    }

    protected static void printConfigExample()
    {
        System.out.println("Copy the following fragment into config file:");
        System.out.println();
        System.out.println("[<WebProxy server IP address>:<port>]");
        System.out.println("mac = <STB MAC address>");
        System.out.println("url = <portal URL (without path)>");
        System.out.println("portal = stalker");
        System.out.println();
        System.out.println("and modify it according to your STB information. For example:");
        System.out.println();
        System.out.println("[192.168.0.10:8088]");
        System.out.println("mac = 00:1A;79:00:00:00");
        System.out.println("url = http://myportal.local");
        System.out.println("portal = stalker");
        System.out.println();

        System.out.println("Use one of the following addresses as the server address:");
        WebServer.listLocalIpAddresses();
    }

    protected static void printHelp()
    {
        int tabValue = 8;
        System.out.println("WebProxy for STB emulator, v." + APP_VERSION);
        System.out.println("Command line options:");
        System.out.println(StringUtils.repeat(" ", tabValue) + "--config=<configuration file>");
    }
}
