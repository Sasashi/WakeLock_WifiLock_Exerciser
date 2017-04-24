package com.darryncampbell.wakelockexample;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    public static final String LOG_TAG = "Wake Lock Test";
    private static final String WAKE_LOCK_TAG_1 = "WakeLockExampleAppTag1";
    private static final String WAKE_LOCK_TAG_2 = "WakeLockExampleAppTag2";
    private static final String WIFI_LOCK_TAG = "WifiLockExampleAppTag";
    private PowerManager.WakeLock wakeLock1 = null;
    private PowerManager.WakeLock wakeLock2 = null;
    private WifiManager.WifiLock wifiLock = null;
    ResponseReceiver receiver;

    private TextView txtOutput = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        txtOutput = (TextView) findViewById(R.id.txtOutput);
        final Button btnWakeLockAcquire = (Button) findViewById(R.id.btnAcquireWakeLock);
        final Button btnWakeLockRelease = (Button) findViewById(R.id.btnReleaseWakeLock);
        final Button btnWifiLockAcquire = (Button) findViewById(R.id.btnAcquireWifiLock);
        final Button btnWifiLockRelease = (Button) findViewById(R.id.btnReleaseWifiLock);
        final Button btnBackgroundServiceStart = (Button) findViewById(R.id.btnStartService);
        final Button btnBackgroundServiceStop = (Button) findViewById(R.id.btnStopService);
        final Button btnConfigureBatteryOptimisation = (Button) findViewById(R.id.btnConfigureBatteryOptimization);

        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wm != null) {
            wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL, WIFI_LOCK_TAG);
            wifiLock.setReferenceCounted(true);
        }
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock1 = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                WAKE_LOCK_TAG_1);
        wakeLock2 = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                WAKE_LOCK_TAG_2);

        //  Uncomment this line to acquire a wake lock without user intervention
        //acquireWakeLock(wakeLock2);

        configureBatteryOptimisation(false);

        //  Button handlers
        btnWakeLockAcquire.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                updateUI("Acquiring Wake Lock");
                acquireWakeLock(wakeLock1);
            }
        });
        btnWakeLockRelease.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                updateUI("Releasing Wake Lock");
                releaseWakeLock(wakeLock1);
            }
        });
        btnWifiLockAcquire.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                updateUI("Acquiring Wifi Lock");
                acquireWifiLock();
            }
        });
        btnWifiLockRelease.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                updateUI("Releasing Wifi Lock");
                releaseWifiLock();
            }
        });
        btnBackgroundServiceStart.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                updateUI("Background service running");
                startBackgroundService();
            }
        });
        btnBackgroundServiceStop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //  We are not going for 100% thread handling amazement here
                updateUI("Stopping Background service (please wait up to 20 seconds)");
                stopBackgroundService();
            }
        });
        btnConfigureBatteryOptimisation.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                updateUI("Launching dialog to configure battery optimisation.  Ensure this application is ignoring optimisations.");
                configureBatteryOptimisation(true);
            }
        });

    }

    @Override
    protected void onResume()
    {
        super.onResume();
        IntentFilter broadcastFilter = new IntentFilter(ResponseReceiver.LOCAL_ACTION);
        receiver = new ResponseReceiver();
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager.registerReceiver(receiver, broadcastFilter);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager.unregisterReceiver(receiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }

    void acquireWakeLock(PowerManager.WakeLock wakeLock)
    {
        if (!wakeLock.isHeld()) {
            wakeLock.acquire();
            updateUI("Wake Lock successfully acquired");
        }
        else
        {
            updateUI("Wake Lock is already acquired");
        }
    }
    void releaseWakeLock(PowerManager.WakeLock wakeLock)
    {
        if (wakeLock.isHeld()) {
            wakeLock.release();
            updateUI("Wake Lock successfully released");
        }
        else
        {
            updateUI("Wake Lock is not acquired");
        }
    }
    void acquireWifiLock()
    {
        if (!wifiLock.isHeld()) {
            wifiLock.acquire();
            updateUI("Wifi Lock successfully acquired");
        }
        else
        {
            updateUI("Wifi Lock is already acquired");
        }
    }
    void releaseWifiLock()
    {
        if (wifiLock.isHeld()) {
            wifiLock.release();
            updateUI("Wifi Lock successfully released");
        }
        else
        {
            updateUI("Wifi Lock is not acquired");
        }
    }

    void startBackgroundService()
    {
        disableUIElements(true);
        final EditText editPostAddress = (EditText) findViewById(R.id.editIpAddress);
        final CheckBox beepCheck = (CheckBox) findViewById(R.id.checkBeep);
        final CheckBox postCheck = (CheckBox) findViewById(R.id.checkPost);
        final Spinner spinnerTimeout = (Spinner) findViewById(R.id.spinnerTimeout);
        boolean bBeep = beepCheck.isChecked();
        boolean bPost = postCheck.isChecked();
        String address = editPostAddress.getText().toString();
        int timeout = 1000;
        if (spinnerTimeout.getSelectedItemId() == 1)
            timeout = 10000;
        else if (spinnerTimeout.getSelectedItemId() == 2)
            timeout = 20000;
        //  Because the beep holds a wake lock, do not allow this to proceed with 1 second
        if (bBeep && timeout == 1000)
        {
            Toast.makeText(this, "Beep holds a partial wake lock for a few seconds after each beep therefore this configuration is not advised", Toast.LENGTH_LONG).show();
            disableUIElements(false);
            return;
        }
        MyIntentService.shouldContinue = true;
        Intent msgIntent = new Intent(MainActivity.this, MyIntentService.class);
//        msgIntent.putExtra(MyIntentService.REQUEST_POST_ADDRESS, "http://192.168.0.2:8082/script.php");
        msgIntent.putExtra(MyIntentService.REQUEST_POST_ADDRESS, address);
        msgIntent.putExtra(MyIntentService.REQUEST_ACTION_BEEP, bBeep);
        msgIntent.putExtra(MyIntentService.REQUEST_ACTION_POST, bPost);
        msgIntent.putExtra(MyIntentService.REQUEST_TIMEOUT, timeout);
        startService(msgIntent);
    }

    void stopBackgroundService()
    {
        MyIntentService.shouldContinue = false; //  We are not going for 100% thread handling amazement here
    }

    void configureBatteryOptimisation(boolean showDialogRegardless)
    {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent();
            String packageName = getPackageName();
            if (pm.isIgnoringBatteryOptimizations(packageName)) {
                updateUI("Battery optimisations are being ignored");
                if (showDialogRegardless) {
                    intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                    startActivity(intent);
                }
            }
            else {
                updateUI("Battery optimisations are in effect");
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                startActivity(intent);
            }
        }

    }

    public void updateUI(String msg) {
        Log.i(LOG_TAG, msg);
        final String str = msg;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtOutput.setText(str);
            }
        });
    }

    public void disableUIElements(boolean disable)
    {
        final Button btnBackgroundServiceStart = (Button) findViewById(R.id.btnStartService);
        final EditText editPostAddress = (EditText) findViewById(R.id.editIpAddress);
        final CheckBox beepCheck = (CheckBox) findViewById(R.id.checkBeep);
        final CheckBox postCheck = (CheckBox) findViewById(R.id.checkPost);
        final Spinner spinnerTimeout = (Spinner) findViewById(R.id.spinnerTimeout);
        final Button btnConfigureBatteryOptimisation = (Button) findViewById(R.id.btnConfigureBatteryOptimization);
        btnBackgroundServiceStart.setEnabled(!disable);
        editPostAddress.setEnabled(!disable);
        beepCheck.setEnabled(!disable);
        postCheck.setEnabled(!disable);
        spinnerTimeout.setEnabled(!disable);
        btnConfigureBatteryOptimisation.setEnabled(!disable);
    }

    public class ResponseReceiver extends BroadcastReceiver {
        public static final String LOCAL_ACTION = "com.darryncampbell.wakelockexampe.intent_service.ALL_DONE";

        @Override
        public void onReceive(Context context, Intent intent)
        {
            updateUI("Service has stopped");
            disableUIElements(false);
        }
    }

}