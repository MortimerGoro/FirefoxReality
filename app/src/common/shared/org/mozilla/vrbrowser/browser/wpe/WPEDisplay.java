package org.mozilla.vrbrowser.browser.wpe;

import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.view.Surface;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;

import com.wpe.wpeview.WPEView;

import org.mozilla.vrbrowser.browser.api.DisplayAPI;

import java.util.LinkedList;

public class WPEDisplay implements DisplayAPI, SurfaceHolder {

    private WPEView mView;
    WPESession mSession;
    Surface mSurface;
    int mWidth;
    int mHeight;
    boolean mSurfaceCreatedCalled ;
    LinkedList<Callback> mCallbacks = new LinkedList<>();

    WPEDisplay(WPEView view, WPESession session) {
        mView = view;
        mSession = session;
    }

    // DisplayAPI
    @Override
    public void surfaceChanged(@NonNull Surface surface, int width, int height) {
        mSurface = surface;
        mWidth = width;
        mHeight = height;
        mView.callSurfaceChanged(surface, width, height);
        mSession.notifyFirstComposite();
        /*if (!mCallbacks.isEmpty()) {
            for (Callback callback: mCallbacks) {
                if (!mSurfaceCreatedCalled) {
                    callback.surfaceCreated(this);
                }
                callback.surfaceChanged(this, PixelFormat.RGBA_8888, width, height);
            }

            mSurfaceCreatedCalled = true;
            mSession.notifyFirstComposite();
        }*/
    }

    @Override
    public void surfaceChanged(@NonNull Surface surface, int left, int top, int width, int height) {
        surfaceChanged(surface, width, height);
    }

    @Override
    public void surfaceDestroyed() {
        mView.callSurfaceDestroyed();
        /*for (Callback callback: mCallbacks) {
            callback.surfaceDestroyed(this);
        }*/
    }


    // SurfaceHolder

    @Override
    public void addCallback(Callback callback) {
        mCallbacks.add(callback);
        if (!mSurfaceCreatedCalled && mSurface != null) {
            surfaceChanged(mSurface, mWidth, mHeight);
        }
    }

    @Override
    public void removeCallback(Callback callback) {
        mCallbacks.remove(callback);
    }

    @Override
    public boolean isCreating() {
        return false;
    }

    @Override
    public void setType(int type) {

    }

    @Override
    public void setFixedSize(int width, int height) {

    }

    @Override
    public void setSizeFromLayout() {

    }

    @Override
    public void setFormat(int format) {

    }

    @Override
    public void setKeepScreenOn(boolean screenOn) {

    }

    @Override
    public Canvas lockCanvas() {
        return null;
    }

    @Override
    public Canvas lockCanvas(Rect dirty) {
        return null;
    }

    @Override
    public void unlockCanvasAndPost(Canvas canvas) {

    }

    @Override
    public Rect getSurfaceFrame() {
        return null;
    }

    @Override
    public Surface getSurface() {
        return mSurface;
    }
}
