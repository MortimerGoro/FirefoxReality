package org.mozilla.vrbrowser.browser.wpe;

import android.view.Surface;

import androidx.annotation.NonNull;

import com.wpe.wpeview.WPEView;

import org.mozilla.vrbrowser.browser.api.DisplayAPI;

public class WPEDisplay implements DisplayAPI {

    private WPEView mView;
    WPEDisplay(WPEView view) {
        mView = view;
    }

    @Override
    public void surfaceChanged(@NonNull Surface surface, int width, int height) {
        mView.callSurfaceChanged(surface, width, height);
    }

    @Override
    public void surfaceChanged(@NonNull Surface surface, int left, int top, int width, int height) {
        mView.callSurfaceChanged(surface, width, height);
    }

    @Override
    public void surfaceDestroyed() {
        mView.callSurfaceDestroyed();
    }
}
