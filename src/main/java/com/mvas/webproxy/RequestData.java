package com.mvas.webproxy;

import com.mvas.webproxy.portals.AbstractRequestHandler;

import javax.servlet.http.HttpServletRequest;
import java.net.URL;
import java.util.HashMap;


public class RequestData {
    protected URL proxyUrl;

    protected URL realUrl;
    protected String target;
    protected String macAddress;

    protected String deviceId;
    protected String serialNumber;
    protected String cookie;
    protected HttpServletRequest request;

    protected HashMap<String, String> queryParams;
    protected HashMap<String, String> headers;

    public String getConnectionName() {
        return connectionName;
    }

    public void setConnectionName(String connectionName) {
        this.connectionName = connectionName;
    }

    protected String connectionName;

    public boolean isEmulator() {
        return isEmulator;
    }

    public void setEmulator(boolean isEmulator) {
        this.isEmulator = isEmulator;
    }

    protected boolean isEmulator;

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }



    public URL getProxyUrl() {
        return proxyUrl;
    }

    public void setProxyUrl(URL proxyUrl) {
        this.proxyUrl = proxyUrl;
    }

    public URL getRealUrl() {
        return realUrl;
    }

    public void setRealUrl(URL realUrl) {
        this.realUrl = realUrl;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress != null ? macAddress.toUpperCase() : null;
    }

    public String getCookie() {
        return cookie;
    }

    public void setCookie(String cookie) {
        this.cookie = cookie;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }

    public HashMap<String, String> getQueryParams() {
        return queryParams;
    }

    public void setQueryParams(HashMap<String, String> queryParams) {
        this.queryParams = queryParams;
    }

    public HashMap<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(HashMap<String, String> headers) {
        this.headers = headers;
    }

    public void setRequestHandler(AbstractRequestHandler requestHandler) {
        this.requestHandler = requestHandler;
    }

    public AbstractRequestHandler getRequestHandler() {
        return requestHandler;
    }

    protected AbstractRequestHandler requestHandler;

    public RequestData()
    {
        headers = new HashMap<>();
        queryParams = new HashMap<>();
    }
}
