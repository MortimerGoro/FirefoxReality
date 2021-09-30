/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.vrbrowser;

import com.huawei.agconnect.AGConnectInstance;
import com.huawei.agconnect.AGConnectOptionsBuilder;
import com.huawei.hvr.LibUpdateClient;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import org.json.JSONObject;
import org.mozilla.vrbrowser.browser.Services;

import java.io.IOException;
import java.io.InputStream;

public class PlatformActivity extends Activity implements SurfaceHolder.Callback {
    public static final String TAG = "PlatformActivity";
    private SurfaceView mView;
    private Context mContext = null;

    static {
        Log.i(TAG, "LoadLibrary");
    }

    public static boolean filterPermission(final String aPermission) {
        return false;
    }

    public static boolean isNotSpecialKey(KeyEvent event) {
        return true;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "PlatformActivity onCreate");
        super.onCreate(savedInstanceState);

        mContext = this;

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);// 设置全屏
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        mView = new SurfaceView(this);
        setContentView(mView);

        mView.getHolder().addCallback(this);
        //getDir();
        new LibUpdateClient(this).runUpdate();
        nativeOnCreate();

        initializeAGConnect();
    }

    private void initializeAGConnect() {
        try {
            AGConnectOptionsBuilder builder = new AGConnectOptionsBuilder();
            InputStream in = getAssets().open("agconnect-services.json");
            builder.setInputStream(in);
            builder.setClientId("727546588758557888");
            builder.setClientSecret("[!0058F906D90C6DB172C6513B6A42B476251E744ADEEDE2D635A8E4CDD32E3CBD6BACD105AF60FE9FDC67F8C6DB6A9153EC9667C045AA80B4AC89C34ADAC1965CDA4E41CC18BA131D8236C2939C456083CB1A3B293699965204BC12A7DD885A3F52]");
            builder.setApiKey("[!00C6EF31FD20C5C565D8144543970C8E8976939863514003DB4EABB9313E971934481502B46CD6A2A8FD49EC9313F27EAF27C097104AB38F9E49EA2E4A6FD7A46117FB61668F4DB7FACD3300198E2C21D96AF0788CACB75440D606F70BCE3ADAAC9F9ABC138FE949E5D656BAD9E0A389AD48EB6462DF72D51A78B3F23D20593ECA]");
            builder.setCPId("5190034000026804879");
            builder.setProductId("737518067793596612");
            builder.setAppId("104792907");

            AGConnectInstance.initialize(this, builder);
            ((VRBrowserApplication)getApplicationContext()).setSpeechRecognizer(new HVRSpeechRecognizer(this));

        } catch (IOException e) {
            Log.e(TAG, "Place the agconnect-services.json file in hvr flavor assets in order to use the HVR SDK");
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "PlatformActivity onDestroy");
        super.onDestroy();
        nativeOnDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "PlatformActivity onStart");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "PlatformActivity onStop");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "PlatformActivity onPause");
        queueRunnable(this::nativeOnPause);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "PlatformActivity onResume");
        queueRunnable(this::nativeOnResume);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.e(TAG, "makelele life surfaceCreated");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.i(TAG, "PlatformActivity surfaceChanged");
        queueRunnable(() -> nativeOnSurfaceChanged(holder.getSurface()));
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
        Log.i(TAG, "PlatformActivity surfaceDestroyed");
        queueRunnable(this::nativeOnSurfaceDestroyed);
    }

    protected boolean platformExit() {
        return false;
    }
    protected native void queueRunnable(Runnable aRunnable);
    protected native void nativeOnCreate();
    protected native void nativeOnDestroy();
    protected native void nativeOnPause();
    protected native void nativeOnResume();
    protected native void nativeOnSurfaceChanged(Surface surface);
    protected native void nativeOnSurfaceDestroyed();
}



