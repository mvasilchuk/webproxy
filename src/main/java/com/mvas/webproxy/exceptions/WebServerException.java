package com.mvas.webproxy.exceptions;

public class WebServerException extends RuntimeException {


    int code;
    public WebServerException(int code)
    {
        this.code = code;
    }
    public int getCode() {
        return code;
    }

}
