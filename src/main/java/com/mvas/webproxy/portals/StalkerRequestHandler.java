package com.mvas.webproxy.portals;


import com.mvas.webproxy.DeviceConnectionInfo;
import com.mvas.webproxy.RequestData;
import com.mvas.webproxy.WebServer;
import com.mvas.webproxy.config.PortalConfiguration;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StalkerRequestHandler implements AbstractRequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(StalkerRequestHandler.class);

    public static final String GET_PARAM_DEVICE_ID = "device_id";
    public static final String GET_PARAM_DEVICE_ID2 = "device_id2";
    public static final String GET_PARAM_SIGNATURE = "signature";
    public static final String GET_PARAM_SESSION = "session";


    public static final String CONFIG_PORTAL_URL = "portal";
    public static final String GET_PARAM_MAC_ADDRESS = "mac";

    public static final String ACTION_HANDSHAKE = "action=handshake";

    Pattern cookiePattern = Pattern.compile(".*mac=(([0-9A-F]{2}[:-]){5}([0-9A-F]{2})).*");
    Pattern handshakePattern = Pattern.compile(ACTION_HANDSHAKE);

    public static final String HEADER_AUTHORIZATION = "Authorization";

    public static final class HeadersList {
        public HashMap<String, String> getList() {
            return list;
        }

        private HashMap<String, String> list = new HashMap<>();
    }

    public static final class ParamsList {
        public HashMap<String, String> getList() {
            return list;
        }

        private HashMap<String, String> list = new HashMap<>();
    }

    public HashMap<String, HeadersList> staticHeadersForMac = new HashMap<>();
    public HashMap<String, ParamsList> staticParamsForMac = new HashMap<>();

    DeviceConnectionInfo deviceConnectionInfo;

    static HashMap<String, Pattern> replacements = new HashMap<>();

    static String[] configOptions;
    static String[] getParamNames;

    static {
        getParamNames = new String[] {
             GET_PARAM_DEVICE_ID, GET_PARAM_DEVICE_ID2, GET_PARAM_SIGNATURE, GET_PARAM_SESSION
        };

        for (String name : getParamNames) {
            replacements.put(name, Pattern.compile("([\\?&])?(" + name + ")=([a-zA-Z0-9/+]*)"));
        }

        configOptions = new String[] {
            GET_PARAM_DEVICE_ID, GET_PARAM_DEVICE_ID2, GET_PARAM_SIGNATURE, GET_PARAM_SESSION, CONFIG_PORTAL_URL, GET_PARAM_MAC_ADDRESS
        };
    }

    private HeadersList getHeadersForRequest(final RequestData requestData)
    {
        return staticHeadersForMac.get(requestData.getMacAddress());
    }

    private ParamsList getParamsForRequest(final RequestData requestData)
    {
        return staticParamsForMac.get(requestData.getMacAddress());
    }

    public StalkerRequestHandler(DeviceConnectionInfo deviceConnectionInfo)
    {
        this.deviceConnectionInfo = deviceConnectionInfo;
    }

    @Override
    public String getURL(final RequestData requestData) {
        logger.debug("StalkerRequestHandler::getURL()");
        String result = requestData.getRealUrl().toString();

        HashMap<String, String> staticParams = getParamsForRequest(requestData).getList();
        for(Map.Entry<String, Pattern> entry: replacements.entrySet())
        {
            Matcher matcher = entry.getValue().matcher(result);
            if(matcher.find())
            {
                String name = matcher.group(2);
                if(staticParams.get(name) == null)
                {
                    String value = matcher.group(3);
                    staticParams.put(name, value);
                    logger.debug("set static param [" + name + "] to [" + value + "]");

                    if(name.equals(GET_PARAM_DEVICE_ID))
                        WebServer.getPortalConfiguration().set(requestData.getConnectionName(), name, value);
                    if(name.equals(GET_PARAM_DEVICE_ID2))
                        WebServer.getPortalConfiguration().set(requestData.getConnectionName(), name, value);
                }
            }
        }

        for(Map.Entry<String, Pattern> rep: replacements.entrySet())
        {
            Pattern from = rep.getValue();
            String to = rep.getKey();
            result = result.replaceAll(from.pattern(), "$1$2=" + staticParams.get(to));
        }
        logger.debug("New query string: " + result);
        return result;
    }

    @Override
    public void onRequest(final RequestData requestData, final URLConnection urlConnection) {
        logger.debug("StalkerRequestHandler::onRequest()");
        HashMap<String, String> staticHeaders = getHeadersForRequest(requestData).getList();

        for(Map.Entry<String, String> header: requestData.getHeaders().entrySet())
        {
            String headerName = header.getKey();
            String headerValue = header.getValue();

            if(headerName.equals(HEADER_AUTHORIZATION))
            {
                if(staticHeaders.get(HEADER_AUTHORIZATION) == null)
                    staticHeaders.put(HEADER_AUTHORIZATION, headerValue);
                else
                {
                    String authorizationHeader = staticHeaders.get(HEADER_AUTHORIZATION);
                    if(headerValue.equals(authorizationHeader))
                        continue;

                    logger.debug("Overwriting [" + HEADER_AUTHORIZATION + "] from {" + headerValue + " } to {" + authorizationHeader + "}");
                    urlConnection.setRequestProperty(HEADER_AUTHORIZATION, authorizationHeader);
                }

            }
        }
    }

    @Override
    public InputStream onResponse(final RequestData requestData, final InputStream iStream) {
        logger.debug("StalkerRequestHandler::onResponse()");

        String target = requestData.getTarget();
        if(!target.contains(".php"))
            return iStream;

        try {
            String data = IOUtils.toString(iStream);
            Matcher matcher = handshakePattern.matcher(requestData.getRealUrl().toString());
            if(matcher.find())
            {
                processHandshake(requestData, data);
            }

            return  IOUtils.toInputStream(data);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return iStream;
    }

    @Override
    public void onBeforeRequest(final RequestData requestData, final PortalConfiguration portalConfiguration) {
        logger.debug("StalkerRequestHandler::onBeforeRequest()");
        String macAddress = requestData.getMacAddress();

        if(macAddress == null || macAddress.isEmpty())
        {
            String cookie = requestData.getCookie().replace("%3A", ":");
            Matcher matcher = cookiePattern.matcher(cookie);
            if(matcher.find())
            {
                requestData.setMacAddress(matcher.group(1));
                macAddress = matcher.group(1);
                logger.debug("MAC: " + matcher.group(1));
            }
            else
                macAddress = "empty";
        }

        if(!staticHeadersForMac.containsKey(macAddress))
            staticHeadersForMac.put(macAddress, new HeadersList());

        HashMap<String, String> staticHeaders =  getHeadersForRequest(requestData).getList();

        String token = portalConfiguration.get(requestData.getConnectionName(), "token");
        if(token != null)
            staticHeaders.put(HEADER_AUTHORIZATION, "Bearer " + token);


        if(!staticParamsForMac.containsKey(macAddress))
        {
            ParamsList params = new ParamsList();
            HashMap<String, String> list = params.getList();
            for(String name: configOptions)
            {
                String value = portalConfiguration.get(requestData.getConnectionName(), name);
                if(value == null)
                {
                    logger.debug("Skipping NULL config value [" + name + "]");
                    continue;
                }

                logger.debug("Loading {" + name + "} -> {" + value + "}");
                list.put(name, value);
            }
            staticParamsForMac.put(macAddress, params);
        }

        try {
            requestData.setRealUrl(new URL(getURL(requestData)));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public void processHandshake(final RequestData requestData, final String data)
    {
        logger.debug("StalkerRequestHandler::processHandshake()");
        HashMap<String, String> staticHeaders =  getHeadersForRequest(requestData).getList();
        JSONObject json;
        try {
            json = new JSONObject(data);
            JSONObject js = json.getJSONObject("js");
            String token = js.getString("token");
            staticHeaders.put(HEADER_AUTHORIZATION, "Bearer " + token);
            WebServer.getPortalConfiguration().set(requestData.getConnectionName(), "token", token);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
