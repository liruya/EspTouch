package com.liruya.esptouch;

import java.net.InetAddress;

public class EsptouchResult {
    private String mAddress;
    private InetAddress mInetAddress;

    public EsptouchResult(String address, InetAddress inetAddress) {
        mAddress = address;
        mInetAddress = inetAddress;
    }

    public String getAddress() {
        return mAddress;
    }

    public InetAddress getInetAddress() {
        return mInetAddress;
    }
}
