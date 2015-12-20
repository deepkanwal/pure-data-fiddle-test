package com.deepkanwal.fiddletest;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import org.puredata.android.io.AudioParameters;
import org.puredata.android.service.PdService;
import org.puredata.android.utils.PdUiDispatcher;
import org.puredata.core.PdBase;
import org.puredata.core.PdListener;
import org.puredata.core.utils.IoUtils;

import java.io.File;
import java.io.IOException;

public class FiddleTest extends AppCompatActivity {

    private PdUiDispatcher dispatcher;
    private PdService pdService = null;

    private final ServiceConnection pdConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            pdService = ((PdService.PdBinder)service).getService();
            try {
                initPd();
                loadPatch();
            } catch (IOException e) {
                Log.e(this.getClass().getName(), e.toString());
                finish();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // this method will never be called
        }
    };

    private void  initPd() throws IOException {
        // Configure the audio glue
        AudioParameters.init(this);
        int sampleRate = AudioParameters.suggestSampleRate();
        pdService.initAudio(sampleRate, 1, 2, 10.0f);
        start();

        // Create and install the dispatcher
        dispatcher = new PdUiDispatcher();
        PdBase.setReceiver(dispatcher);
        dispatcher.addListener("pitch", new PdListener.Adapter() {
            @Override
            public void receiveFloat(String source, final float x) {
                Log.i(this.getClass().getName(), "Recieved Pitch: " + x);
            }
        });
    }

    private void start() {
        if (!pdService.isRunning()) {
            Intent intent = new Intent(FiddleTest.this,
                    FiddleTest.class);
            pdService.startAudio(intent, R.drawable.icon,
                    "FiddleTest", "Return to FiddleTest.");
        }
    }

    private void loadPatch() throws IOException {
        File dir = getFilesDir();
        IoUtils.extractZipResource(
                getResources().openRawResource(R.raw.fiddle_test), dir, true);
        File patchFile = new File(dir, "fiddle_test.pd");
        PdBase.openPatch(patchFile.getAbsolutePath());
    }

    private void initSystemServices() {
        TelephonyManager telephonyManager =
                (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                if (pdService == null) return;
                if (state == TelephonyManager.CALL_STATE_IDLE) {
                    start();
                } else {
                    pdService.stopAudio();
                }
            }
        }, PhoneStateListener.LISTEN_CALL_STATE);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initSystemServices();
        bindService(new Intent(this, PdService.class), pdConnection, BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindService(pdConnection);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_fiddle_test, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
