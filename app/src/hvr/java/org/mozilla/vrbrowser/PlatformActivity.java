/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.vrbrowser;

import com.huawei.hvr.LibUpdateClient;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

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
        Log.i(TAG, "onCreate");
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
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "makelele onDestroy");
        super.onDestroy();
        nativeOnDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        queueRunnable(this::nativeOnPause);
    }

    @Override
    protected void onResume() {
        super.onResume();
        queueRunnable(this::nativeOnResume);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.i(TAG, "makelele surfaceCreated");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.i(TAG, "makelele surfaceChanged");
        queueRunnable(() -> nativeOnSurfaceChanged(holder.getSurface()));
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
        Log.i(TAG, "makelele surfaceDestroyed");
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



