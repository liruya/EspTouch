package com.liruya.esptouch;

import android.net.wifi.WifiInfo;

import java.util.List;

public interface IEsptouchLinkListener {
    void onWifiChanged(WifiInfo wifiInfo);

    void onLocationChanged();

    void onStart();

    void onFailed();

    void onLinked(EsptouchResult result);

    void onCompleted(List<EsptouchResult> results);
}
