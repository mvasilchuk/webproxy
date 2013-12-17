package com.mvas.webproxy;

import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * This class holds information about all connections from device to server
 *
 * */
public class DeviceConnectionList {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(DeviceConnectionList.class);

    /**
     * 0 - Record name (protocol|hostname|mac-address). See {@link DefaultHandler#CONNECTION_NAME_FORMAT}.
     * 1 - Connection info
     */
    HashMap<String, DeviceConnectionInfo> infoList = new HashMap<>();

    public DeviceConnectionList()
    {

    }

    public DeviceConnectionInfo getConnectionInfo(String id)
    {
        return infoList.get(id);
    }

    public void addConnectionInfo(final String name, final DeviceConnectionInfo info)
    {
        infoList.put(name, info);
    }

    // For debug purposes
    public void printList()
    {
        for(Map.Entry<String, DeviceConnectionInfo> entry: infoList.entrySet())
        {
            logger.debug("Device connection: " + entry.getKey() + " -> " + entry.getValue());
        }
    }
}
