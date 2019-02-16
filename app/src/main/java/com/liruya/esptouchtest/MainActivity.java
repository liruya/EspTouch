package com.liruya.esptouchtest;

import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.os.Build;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.support.design.widget.TextInputEditText;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.liruya.base.BaseActivity;
import com.liruya.esptouch.EsptouchLinker;
import com.liruya.esptouch.EsptouchResult;
import com.liruya.esptouch.IEsptouchLinkListener;

import java.util.List;

public class MainActivity extends BaseActivity {

    private final int CODE_REQUEST_LOCATION_PERMISSION = 1;
    private TextInputEditText main_et_ssid;
    private TextInputEditText main_et_password;
    private TextView main_tv_msg;
    private ImageButton main_ib_change;
    private ProgressBar main_progress;
    private ToggleButton main_tb_start;

    private EsptouchLinker mEsptouchLinker;
    private IEsptouchLinkListener mLinkListener;
    private CountDownTimer mTimer;

    @Override
    protected void onStart() {
        super.onStart();

        initData();
        initEvent();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mEsptouchLinker != null) {
            mEsptouchLinker.unregisterWiFiStateChangedListener();
        }
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_main;
    }

    @Override
    protected void initView() {
        main_et_ssid = findViewById(R.id.main_et_ssid);
        main_et_password = findViewById(R.id.main_et_password);
        main_tv_msg = findViewById(R.id.main_tv_msg);
        main_ib_change = findViewById(R.id.main_ib_change);
        main_progress = findViewById(R.id.main_progress);
        main_tb_start = findViewById(R.id.main_tb_start);
    }

    @Override
    protected void initData() {
        main_et_password.requestFocus();
        mEsptouchLinker = new EsptouchLinker(this);
        mLinkListener = new IEsptouchLinkListener() {
            @Override
            public void onWifiChanged(WifiInfo wifiInfo) {
                if (wifiInfo == null) {
                    return;
                }
                String ssid = wifiInfo.getSSID();
                if (ssid.length() >= 2 && ssid.startsWith("\"") && ssid.endsWith("\"")) {
                    ssid = ssid.substring(1, ssid.length() - 1);
                }
                boolean connected = !TextUtils.isEmpty(wifiInfo.getBSSID());
                boolean is5G = false;
                if (connected && Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP && wifiInfo.getFrequency() > 4900 && wifiInfo.getFrequency() < 5900) {
                    is5G = true;
                }
                main_et_ssid.setText(ssid);
                main_tb_start.setEnabled(connected && !is5G);
                if (connected) {
                    if (is5G) {
                        main_tv_msg.setText("The device does not support 5G!");
                    } else {
                        main_tv_msg.setText("");
                        main_et_password.requestFocus();
                    }
                } else {
                    main_tv_msg.setText("Please connect router.");
                }
                main_tb_start.setChecked(false);
                stopEsptouch();
            }

            @Override
            public void onLocationChanged() {
                Log.e(TAG, "onLocationChanged: " );
            }

            @Override
            public void onStart() {

            }

            @Override
            public void onFailed() {

            }

            @Override
            public void onLinked(final EsptouchResult result) {
                main_tv_msg.append("<" + result.getAddress() + "> is connected to router. IP: " + result.getInetAddress().getHostAddress() + "\n");
            }

            @Override
            public void onCompleted(List<EsptouchResult> results) {
                main_tb_start.setChecked(false);
                stopEsptouch();
                if (results == null || results.size() == 0) {
                    main_tv_msg.append("No device connected to router.");
                } else {
                    main_tv_msg.append("" + results.size() + " devices connected to " + getSsidText());
                }
            }
        };
        mEsptouchLinker.setEsptouchLinkListener(mLinkListener);
        mEsptouchLinker.registerWiFiStateChangedListener();

        mTimer = new CountDownTimer(mEsptouchLinker.getPeriod(), 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int progress = (int) ((mEsptouchLinker.getPeriod() - millisUntilFinished) / 1000);
                main_progress.setProgress(progress);
            }

            @Override
            public void onFinish() {
                main_progress.setProgress(main_progress.getMax());
            }
        };
    }

    @Override
    protected void initEvent() {
        main_ib_change.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gotoSystemWifiSettings();
            }
        });

        main_tb_start.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    startEsptouch();
                } else {
                    stopEsptouch();
                }
            }
        });
    }

    private void startEsptouch() {
        String ssid = getSsidText();
        String mac =WifiUtil.getGatewayMacAddress(this);
        String psw = getPasswordText();
        Log.e(TAG, "startEsptouch: " + ssid + "  " + psw + "  " + mac );
        main_et_password.setEnabled(false);
        main_ib_change.setEnabled(false);
        main_tv_msg.setText("");
        main_progress.setProgress(0);
        main_progress.setMax(mEsptouchLinker.getPeriod()/1000);
        main_progress.setVisibility(View.VISIBLE);
        mEsptouchLinker.start(ssid, mac, psw, 2, true);
        mTimer.start();
    }

    private void stopEsptouch() {
        mTimer.cancel();
        mEsptouchLinker.stop();
        main_et_password.setEnabled(true);
        main_ib_change.setEnabled(true);
        main_tv_msg.setText("");
        main_progress.setProgress(0);
        main_progress.setVisibility(View.GONE);
    }

    private String getSsidText() {
        return main_et_ssid.getText().toString();
    }

    private String getPasswordText() {
        return main_et_password.getText().toString();
    }

    private void gotoSystemWifiSettings() {
        Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}
