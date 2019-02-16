package com.liruya.esptouch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

import com.espressif.iot.esptouch.EsptouchTask;
import com.espressif.iot.esptouch.IEsptouchListener;
import com.espressif.iot.esptouch.IEsptouchResult;
import com.espressif.iot.esptouch.IEsptouchTask;
import com.espressif.iot.esptouch.task.EsptouchTaskParameter;
import com.espressif.iot.esptouch.task.IEsptouchTaskParameter;
import com.espressif.iot.esptouch.util.ByteUtil;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class EsptouchLinker {
    private final String TAG = "EspTouchLinker";

    private WeakReference<AppCompatActivity> mActivity;

    // without the lock, if the user tap confirm and cancel quickly enough,
    // the bug will arise. the reason is follows:
    // 0. task is starting created, but not finished
    // 1. the task is cancel for the task hasn't been created, it do nothing
    // 2. task is created
    // 3. Oops, the task should be cancelled, but it is running
    private final Object mLock = new Object();
    private IEsptouchListener mListener;
    private AsyncTask<byte[], Void, List<IEsptouchResult>> mTask;
    private IEsptouchTask mEsptouchTask;
    private IEsptouchLinkListener mEsptouchLinkListener;
    private IEsptouchTaskParameter mParameter;

    private boolean mRegistered;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                return;
            }
            WifiManager wifiManager = (WifiManager) mActivity.get().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null && mEsptouchLinkListener != null) {
                switch (action) {
                    case WifiManager.NETWORK_STATE_CHANGED_ACTION:
                        mEsptouchLinkListener.onWifiChanged(wifiManager.getConnectionInfo());
                        break;
                    case LocationManager.PROVIDERS_CHANGED_ACTION:
                        mEsptouchLinkListener.onWifiChanged(wifiManager.getConnectionInfo());
                        mEsptouchLinkListener.onLocationChanged();
                        break;
                }
            }
        }
    };

    private final int ESPTOUCH_COUNT_MIN = 1;
    private final int ESPTOUCH_COUNT_MAX = 10;

    public EsptouchLinker(AppCompatActivity activity) {
        mActivity = new WeakReference<>(activity);
        mListener = new IEsptouchListener() {
            @Override
            public void onEsptouchResultAdded(final IEsptouchResult result) {
                if (mEsptouchLinkListener != null) {
                    mActivity.get().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mEsptouchLinkListener.onLinked(new EsptouchResult(result.getBssid(), result.getInetAddress()));
                        }
                    });
                }
            }
        };
    }
    public void setEsptouchLinkListener(IEsptouchLinkListener listener) {
        mEsptouchLinkListener = listener;
    }

    public void start(String apname, String mac, String psw, int count, boolean broadcast) {
        if (TextUtils.isEmpty(apname) || TextUtils.isEmpty(mac)) {
            return;
        }
        byte[] ssid = ByteUtil.getBytesByString(apname);
        byte[] bssid = ByteUtil.getBytesByString(mac);
        byte[] password = ByteUtil.getBytesByString(psw);
        if (count < ESPTOUCH_COUNT_MIN) {
            count = ESPTOUCH_COUNT_MIN;
        }
        if (count > ESPTOUCH_COUNT_MAX) {
            count = ESPTOUCH_COUNT_MAX;
        }
        mTask = new AsyncTask<byte[], Void, List<IEsptouchResult>>() {
            @Override
            protected void onPreExecute() {
                if (mEsptouchLinkListener != null) {
                    mEsptouchLinkListener.onStart();
                }
            }

            @Override
            protected List<IEsptouchResult> doInBackground(byte[]... bytes) {
                int resultCount;
                synchronized (mLock) {
                    byte[] ssid = bytes[0];
                    byte[] bssid = bytes[1];
                    byte[] password = bytes[2];
                    byte[] count = bytes[3];
                    byte[] broadcast = bytes[4];
                    resultCount = count.length == 0 ? -1 : count[0];
                    mEsptouchTask = new EsptouchTask(ssid, bssid, password, mActivity.get());
                    mEsptouchTask.setPackageBroadcast(broadcast[0] == 1);
                    mEsptouchTask.setEsptouchListener(mListener);
                }
                return mEsptouchTask.executeForResults(resultCount);
            }

            @Override
            protected void onPostExecute(List<IEsptouchResult> results) {
                if (mEsptouchLinkListener != null) {
                    if (results == null || results.size() == 0) {
                        mEsptouchLinkListener.onFailed();
                    } else {
                        IEsptouchResult first = results.get(0);
                        List<EsptouchResult> resultList = new ArrayList<>();
                        if (!first.isCancelled()) {
                            if (first.isSuc()) {
                                for (IEsptouchResult result : results) {
                                    resultList.add(new EsptouchResult(result.getBssid(), result.getInetAddress()));
                                }
                            }
                        }
                        mEsptouchLinkListener.onCompleted(resultList);
                    }
                }
            }
        };
        mTask.execute(ssid, bssid, password, new byte[]{(byte) count}, new byte[]{(byte) (broadcast ? 0x01 : 0x00)});
    }

    public void stop() {
        if (mTask != null) {
            mTask.cancel(true);
            mTask = null;
        }
        if (mEsptouchTask != null) {
            mEsptouchTask.interrupt();
        }
    }

    public int getPeriod() {
        if (mParameter == null) {
            mParameter = new EsptouchTaskParameter();
        }
        return mParameter.getWaitUdpTotalMillisecond();
    }

    public void registerWiFiStateChangedListener() {
        IntentFilter filter = new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            filter.addAction(LocationManager.PROVIDERS_CHANGED_ACTION);
        }
        mActivity.get().registerReceiver(mReceiver, filter);
        mRegistered = true;
    }

    public void unregisterWiFiStateChangedListener() {
        if (mRegistered) {
            mActivity.get().unregisterReceiver(mReceiver);
            mRegistered = false;
        }
    }
}
