package com.mvas.webproxy;

import com.mvas.webproxy.common.Utils;
import com.mvas.webproxy.config.PortalConfiguration;
import com.mvas.webproxy.portals.AbstractRequestHandler;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class DefaultHandler extends AbstractHandler {
    private Logger logger = LoggerFactory.getLogger(DefaultHandler.class);

    public ContextHandler getContext() {
        return context;
    }

    ContextHandler context;
    DeviceConnectionList connectionList = new DeviceConnectionList();

    public static final String CLONING_PAGE= "clone.html";


    private static final String CONNECTION_NAME_FORMAT = "%1$s";

    public static final String X_HEADER_PREFIX = "X-Emulator-";
    public static final String X_REQUESTED_WITH = "X-Requested-With";
    public static final String EMULATOR_PACKAGE_NAME = "com.vasilchmax";

    public DefaultHandler(ContextHandler context)
    {
        this.context = context;
    }

    protected RequestData parseRequest(String target, Request baseRequest, HttpServletRequest request,
                                       HttpServletResponse response)
    {
        logger.debug("DefaultHandler::parseRequest()");
        RequestData requestData = new RequestData();
        requestData.target = target;
        requestData.request = request;

        Enumeration<String> headerNames = request.getHeaderNames();
        while(headerNames.hasMoreElements())
        {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);

            if(headerName.startsWith(X_HEADER_PREFIX))
            {
                try {
                    switch(headerName)
                    {
                        case WebServer.HEADERS.X_EMULATOR_REAL_URL:
                            requestData.setRealUrl(new URL(headerValue));
                            break;
                        case WebServer.HEADERS.X_EMULATOR_PROXY_URL:
                            requestData.setProxyUrl(new URL(headerValue));
                            break;
                        case WebServer.HEADERS.X_EMULATOR_MAC_ADDRESS:
                            requestData.setMacAddress(headerValue);
                            break;
                        case WebServer.HEADERS.X_EMULATOR_DEVICE_ID:
                            requestData.setDeviceId(headerValue);
                            break;
                        case WebServer.HEADERS.X_EMULATOR_SERIAL_NUMBER:
                            requestData.setSerialNumber(headerValue);
                            break;
                        case WebServer.HEADERS.X_EMULATOR_CONNECTION_NAME:
                            requestData.setConnectionName(headerValue);
                            break;
                        default:
                            logger.warn("Unknown header: " + headerName);
                            break;
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }

            }
            else
            {
                if(headerName.equals("Cookie"))
                    requestData.setCookie(headerValue);
                requestData.headers.put(headerName, headerValue);
            }

            //logger.debug("header: [" + headerName + ":" + headerValue + "]");
        }

        String[] queryArray = request.getQueryString() != null ? request.getQueryString().split("&") : new String[]{};
        for(String str: queryArray)
        {
            String[] data = str.split("=");
            if(data.length == 1)
                requestData.queryParams.put(data[0], "");
            else if(data.length == 2)
                requestData.queryParams.put(data[0], data[1]);
            else
                requestData.queryParams.put(data[0], str.substring(0, data[0].length()));
        }

        return requestData;
    }

    public synchronized DeviceConnectionInfo getConnectionInfo(final String connectionName, final HttpServletRequest request)
    {
        logger.debug("DefaultHandler::getConnectionInfo()");
        DeviceConnectionInfo connection = connectionList.getConnectionInfo(connectionName);
        if(connection == null)
        {
            connection = new DeviceConnectionInfo();
            connection.name = connectionName;
            connectionList.addConnectionInfo(connectionName, connection);
        }
        //connectionList.printList();
        return connection;
    }


    @Override
    public void handle(final String target, final Request baseRequest, final HttpServletRequest request,
                       final HttpServletResponse response)
            throws IOException, ServletException {
        logger.debug("DefaultHandler::handle()");
        //Current request data
        RequestData requestData = parseRequest(target, baseRequest, request, response);
        requestData.setConnectionName(request.getServerName() + ":" + request.getServerPort());

        if(requestData.realUrl != null)
        {
            requestData.setEmulator(true);
        }
        else if(target.length() > 1)
        {
            requestData.setEmulator(false);
        }

        logger.debug("connectionName:" + requestData.getConnectionName());

        if(requestData.getConnectionName() != null)
        {

            try {

                processRequest(requestData, target, baseRequest, request, response);
            }
            catch (MalformedURLException e)
            {
                try {
                    InputStream inputStream = context.getBaseResource().getResource("404.html").getInputStream();
                    OutputStream output = response.getOutputStream();
                    IOUtils.copy(inputStream, output);
                    output.flush();
                } catch (IOException e2) {
                    e.printStackTrace();
                }
            }

        }
        else
        {
            logger.debug("CLONE!");

            //We use either real device or emulator, but without proxy enabled
            //Going to cloning page
            try {
                InputStream inputStream = context.getBaseResource().getResource(CLONING_PAGE).getInputStream();
                OutputStream output = response.getOutputStream();
                IOUtils.copy(inputStream, output);
                output.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //baseRequest.setHandled(true);

    }

    public static final int FILE_TYPE_UNKNOWN = 0;
    public static final int FILE_TYPE_TEXT = 1;

    protected  void processRequest(final RequestData requestData,
                                   final String target, final Request baseRequest, final HttpServletRequest request,
                                   final HttpServletResponse response) throws MalformedURLException {
        //We use emulator

        logger.debug("DefaultHandler::processRequest()");

        DeviceConnectionInfo connection = getConnectionInfo(requestData.getConnectionName(), request);

        PortalConfiguration portalConfiguration = WebServer.getPortalConfiguration();
        portalConfiguration.loadConfiguration(requestData);

        AbstractRequestHandler requestHandler = portalConfiguration.getHandler(connection, requestData);
        requestData.setRequestHandler(requestHandler);

        if(!requestData.isEmulator())
        {
            String queryString = request.getQueryString();
            requestData.setRealUrl(new URL(requestData.getRealUrl().toString() + target + (queryString != null ? "?" + queryString: "") ));
        }



        if(requestHandler != null)
            requestHandler.onBeforeRequest(requestData, portalConfiguration);

        try {

            URLConnection conn;
            conn = requestData.getRealUrl().openConnection();

            logger.debug("baseUrl: " + conn.getURL().toString());

            for(Map.Entry<String, String> header: requestData.getHeaders().entrySet())
            {
                conn.addRequestProperty(header.getKey(), header.getValue());
            }

            connection.connection = conn;

            if(requestHandler != null)
                requestHandler.onRequest(requestData, conn);

            InputStream in = conn.getInputStream();
            String contentType = conn.getHeaderField("Content-Type").toLowerCase();
            logger.debug("Content-Type: " + contentType);

            // Only parse text files.
            if( contentType.contains("html")
                || contentType.contains("javascript")
                || contentType.contains("json")
                || contentType.contains("xml")
                || contentType.contains("text"))
            {
                // Trying to decompress possibly gzipped stream
                in = Utils.decompressStream(in);
                if(requestHandler != null)
                    in = requestHandler.onResponse(requestData, in);
            }

            copyHeaders(conn, response);


            OutputStream os;
            if(in instanceof GZIPInputStream)
            {
                os = new GZIPOutputStream(response.getOutputStream());
            }
            else
            {
                os = response.getOutputStream();
            }
            IOUtils.copy(in, os);
            os.flush();
            in.close();
            os.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void copyHeaders(final URLConnection connection, final HttpServletResponse response)
    {
        logger.debug("DefaultHandler::copyHeaders()");
        for(Map.Entry<String, List<String>> entry: connection.getHeaderFields().entrySet())
        {
            String name = entry.getKey();
            for(String value: entry.getValue())
            {
                if(name != null)
                {
                    if(name.equals(X_REQUESTED_WITH) && value.equals(EMULATOR_PACKAGE_NAME))
                        continue;
                    response.setHeader(name, value);
                }
            }
        }
    }

    public static boolean isGzipStream(byte[] bytes) {
        int head = ((int) bytes[0] & 0xff) | ((bytes[1] << 8) & 0xff00);
        return (GZIPInputStream.GZIP_MAGIC == head);
    }


}