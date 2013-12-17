package com.mvas.webproxy.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.zip.GZIPInputStream;


public final class Utils {
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
}
