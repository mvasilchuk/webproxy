package com.mvas.webproxy.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.zip.GZIPInputStream;


public final class Utils {
    private static final Logger logger = LoggerFactory.getLogger(Utils.class);
    public static InputStream decompressStream(final InputStream input) throws IOException {
        PushbackInputStream pb = new PushbackInputStream( input, 2 ); //we need a pushbackstream to look ahead

        byte [] signature = new byte[2];
        //noinspection ResultOfMethodCallIgnored
        int count = pb.read( signature ); //read the signature
        if(count != 2)
            logger.warn("Stream length is less then 2 bytes!");
        pb.unread( signature ); //push back the signature to the stream

        if( signature[ 0 ] == (byte) 0x1f && signature[ 1 ] == (byte) 0x8b ) //check if matches standard gzip magic number
            return new GZIPInputStream( pb );
        else
            return pb;
    }
}
