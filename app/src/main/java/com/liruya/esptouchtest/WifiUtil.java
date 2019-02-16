package com.liruya.esptouchtest;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.annotation.NonNull;

public class WifiUtil {
    public static String getIp(int ip) {
        StringBuffer sb = new StringBuffer();
        sb.append(ip & 0xFF)
          .append('.');
        sb.append((ip >> 8) & 0xFF)
          .append(".");
        sb.append((ip >> 16) & 0xFF)
          .append(".");
        sb.append((ip >> 24) & 0xFF);
        String result = new String(sb);
        return result;
    }

    private static NetworkInfo getWifiNetworkInfo(@NonNull Context context) {
        if (context == null) {
            return null;
        }
        ConnectivityManager mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (mConnectivityManager == null) {
            return null;
        }
        NetworkInfo mWiFiNetworkInfo = mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return mWiFiNetworkInfo;
    }

    private static WifiInfo getConnectedWiFiInfo(@NonNull Context context) {
        if (context == null) {
            return null;
        }
        WifiManager mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (mWifiManager == null) {
            return null;
        }
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        return wifiInfo;
    }

    public static boolean isWiFiConnected(Context context) {
        NetworkInfo mWiFiNetworkInfo = getWifiNetworkInfo(context);
        boolean isWifiConnected = false;
        if (mWiFiNetworkInfo != null) {
            isWifiConnected = mWiFiNetworkInfo.isConnected();
        }
        return isWifiConnected;
    }

    public static String getGatewayMacAddress(@NonNull Context context) {
        if (context == null) {
            return null;
        }
        WifiInfo wifiInfo = getConnectedWiFiInfo(context);
        String bssid = null;
        if (wifiInfo != null && isWiFiConnected(context)) {
            bssid = wifiInfo.getBSSID();
        }
        return bssid;
    }

    public static String getGatewaySsid(@NonNull Context context) {
        if (context == null) {
            return null;
        }
        WifiInfo wifiInfo = getConnectedWiFiInfo(context);
        String ssid = null;
        if (wifiInfo != null && isWiFiConnected(context)) {
            ssid = wifiInfo.getSSID();
        }
        return ssid;
    }

    public static String getGatewaySsid(WifiInfo wifiInfo) {
        String ssid = null;
        if (wifiInfo != null) {
            ssid = wifiInfo.getSSID();
        }
        return ssid;
    }

    public static String getGatewayIp(@NonNull Context context) {
        if (context == null) {
            return null;
        }
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) {
            return null;
        }
        DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
        if (dhcpInfo != null) {
            return getIp(dhcpInfo.gateway);
        }
        return null;
    }

    public static String getLocalMacAddress(@NonNull Context context) {
        if (context == null) {
            return null;
        }
        WifiInfo wifiInfo = getConnectedWiFiInfo(context);
        String mac = null;
        if (wifiInfo != null && isWiFiConnected(context)) {
            mac = wifiInfo.getMacAddress();
        }
        return mac;
    }

    public static String getLocalIp(@NonNull Context context) {
        if (context == null) {
            return null;
        }
        WifiInfo wifiInfo = getConnectedWiFiInfo(context);
        if (wifiInfo != null && isWiFiConnected(context)) {
            int a = wifiInfo.getIpAddress();
            return getIp(a);
        }
        return null;
    }
}
