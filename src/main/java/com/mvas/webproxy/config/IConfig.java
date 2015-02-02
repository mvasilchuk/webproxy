package com.mvas.webproxy.config;


import java.io.IOException;
import java.util.ArrayList;

public interface IConfig {
    void init() throws IOException;
    public String get(final String group, final String name);
    public String set(final String group, final String name, final Object value);
    public boolean has(final String group, final String name);
    public ArrayList<String> getGroups();
}
