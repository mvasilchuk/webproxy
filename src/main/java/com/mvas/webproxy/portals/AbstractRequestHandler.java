package com.mvas.webproxy.portals;

import com.mvas.webproxy.RequestData;
import com.mvas.webproxy.config.PortalConfiguration;

import java.io.InputStream;
import java.net.URLConnection;


public abstract interface AbstractRequestHandler {

    public String getURL(final RequestData requestData);
    public void onRequest(final RequestData requestData, final URLConnection urlConnection);
    public InputStream onResponse(final RequestData requestData, final InputStream iStream);
    public void onBeforeRequest(final RequestData requestData, final PortalConfiguration portalConfiguration);
}
