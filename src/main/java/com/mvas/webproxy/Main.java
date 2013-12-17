package com.mvas.webproxy;


import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.BasicConfigurator;

public class Main {

    public static final String APP_VERSION = "1.0";
    public static final String APP_DESCRIPTION = "WebProxy for STB Emulator";
    public static final String APP_VERSION_STRING = "version " + APP_VERSION;
    public static final String APP_COPYRIGHT = "Copyright(c) 2013 by Maxim Vasilchuk";

    private static final int LINE_WIDTH = 80;
    private static final int INNER_WIDTH = LINE_WIDTH - 2;

    static {
        BasicConfigurator.configure();
    }

    public static void main(String[] args)
    {
        printHeader();
        WebServer.runWebServer(args);
    }

    private static void printHeader()
    {
        System.out.println(StringUtils.repeat("=", LINE_WIDTH));
        System.out.println("=" + StringUtils.center(APP_DESCRIPTION,    INNER_WIDTH) + "=");
        System.out.println("=" + StringUtils.center(APP_VERSION_STRING, INNER_WIDTH) + "=");
        System.out.println("=" + StringUtils.center(APP_COPYRIGHT,      INNER_WIDTH) + "=");
        System.out.println(StringUtils.repeat("=", LINE_WIDTH));
    }
}
