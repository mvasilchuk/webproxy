package com.mvas.webproxy;

import com.mvas.webproxy.config.Config;
import com.mvas.webproxy.config.IniConfig;
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
import java.io.PushbackInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

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

    public static final class HEADERS {
        public static final String X_EMULATOR_MAC_ADDRESS = X_HEADER_PREFIX + "Mac-Address";
        public static final String X_EMULATOR_DEVICE_ID = X_HEADER_PREFIX + "Device-Id";
        public static final String X_EMULATOR_SERIAL_NUMBER = X_HEADER_PREFIX + "Serial-Number";

        public static final String X_EMULATOR_REAL_URL = X_HEADER_PREFIX + "Real-Url";
        public static final String X_EMULATOR_PROXY_URL = X_HEADER_PREFIX + "Proxy-Url";
        public static final String X_EMULATOR_VERSION = X_HEADER_PREFIX + "Version";
        public static final String X_EMULATOR_CONNECTION_NAME = X_HEADER_PREFIX + "Connection-Name";
    }

    protected Config config;
    public DefaultHandler(ContextHandler context)
    {
        this.context = context;

        config = Config.getInstance(new IniConfig());
    }

    protected RequestData parseRequest(String target, Request baseRequest, HttpServletRequest request,
                                       HttpServletResponse response)
    {
        logger.debug("-- DefaultHandler::parseRequest --");
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
                        case HEADERS.X_EMULATOR_REAL_URL:
                            requestData.setRealUrl(new URL(headerValue));
                            break;
                        case HEADERS.X_EMULATOR_PROXY_URL:
                            requestData.setProxyUrl(new URL(headerValue));
                            break;
                        case HEADERS.X_EMULATOR_MAC_ADDRESS:
                            requestData.setMacAddress(headerValue);
                            break;
                        case HEADERS.X_EMULATOR_DEVICE_ID:
                            requestData.setDeviceId(headerValue);
                            break;
                        case HEADERS.X_EMULATOR_SERIAL_NUMBER:
                            requestData.setSerialNumber(headerValue);
                            break;
                        case HEADERS.X_EMULATOR_CONNECTION_NAME:
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
        logger.debug("-- DefaultHandler::getConnectionInfo --");
        DeviceConnectionInfo connection = connectionList.getConnectionInfo(connectionName);
        if(connection == null)
        {
            connection = new DeviceConnectionInfo();
            connection.name = connectionName;
            connectionList.addConnectionInfo(connectionName, connection);
        }
        connectionList.printList();
        return connection;
    }


    @Override
    public void handle(final String target, final Request baseRequest, final HttpServletRequest request,
                       final HttpServletResponse response)
            throws IOException, ServletException {
        logger.debug("TARGET: " + target);
        logger.debug("-- DefaultHandler::handle --");
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

        logger.debug("-- processRequest --");

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
            if(requestHandler != null)
            {
                conn = requestData.getRealUrl().openConnection();
            }
            else
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
            if(contentType.contains("html") || contentType.contains("javascript") || contentType.contains("json") || contentType.contains("xml") || contentType.contains("text"))
            {
                // Trying to decompress possibly gzipped stream
                in = decompressStream(in);
                if(requestHandler != null)
                    in = requestHandler.onResponse(requestData, in);

                boolean isGzipped = false;
                if(in instanceof GZIPInputStream)
                {
                    isGzipped = true;
                    logger.debug("GZIPPED!!!");
                }

                if(isGzipped)
                {
                    String data = IOUtils.toString(in);

                    //logger.debug(data);

                    logger.debug("URL:" + conn.getURL().toString());
                    for(Map.Entry<String, List<String>> entry: conn.getHeaderFields().entrySet())
                    {
                        String name = entry.getKey();
                        for(String value: entry.getValue())
                        {
                            if(name != null)
                            {
                                if(name.equals(X_REQUESTED_WITH) && value.equals(EMULATOR_PACKAGE_NAME))
                                    continue;
                                //logger.debug("In: [" + name + ":" + value + "]");
                                else if(name.equals("Transfer-Encoding"))
                                {
                                    //logger.debug("Transfer-Encoding: " + value);
                                    continue;
                                }
                                else if(name.equals("Content-Length"))
                                {
                                    logger.debug("Content length: " + value);
                                    value = String.valueOf(data.length());
                                }
                                else if(name.equals("Content-Encoding"))
                                {
                                    //logger.debug("Content-Encoding: " + value);
                                    continue;
                                }
                                response.setHeader(name, value);
                            }

                        }

                    }
                    in = IOUtils.toInputStream(data);
                }
                else
                    copyHeaders(conn, response);
            }
            // Just copy all other files
            else
            {
                copyHeaders(conn, response);
            }

            OutputStream os = response.getOutputStream();
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
        for(Map.Entry<String, List<String>> entry: connection.getHeaderFields().entrySet())
        {
            String name = entry.getKey();
            for(String value: entry.getValue())
            {
                if(name != null)
                {
                    if(name.equals(X_REQUESTED_WITH) && value.equals(EMULATOR_PACKAGE_NAME))
                        continue;
                    if(name.equals("Transfer-Encoding"))
                    {
                        logger.debug("Transfer-Encoding: " + value);
                        continue;
                    }
                    //logger.debug(name + ":" + value);
                    response.setHeader(name, value);
                }
            }
        }
    }

    /**
     * Method to decompress GZip input stream
     *
    */
    public static InputStream decompressStream(final InputStream input) throws IOException {
        PushbackInputStream pb = new PushbackInputStream( input, 2 ); //we need a pushbackstream to look ahead

        byte [] signature = new byte[2];
        //noinspection ResultOfMethodCallIgnored
        pb.read( signature ); //read the signature
        pb.unread( signature ); //push back the signature to the stream

        if( signature[ 0 ] == (byte) 0x1f && signature[ 1 ] == (byte) 0x8b ) //check if matches standard gzip magic number
            return new GZIPInputStream( pb );
        else
            return pb;
    }

    public static boolean isGzipStream(byte[] bytes) {
        int head = ((int) bytes[0] & 0xff) | ((bytes[1] << 8) & 0xff00);
        return (GZIPInputStream.GZIP_MAGIC == head);
    }


}