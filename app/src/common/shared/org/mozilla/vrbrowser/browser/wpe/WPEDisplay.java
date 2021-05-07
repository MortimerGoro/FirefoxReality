package org.mozilla.vrbrowser.browser.wpe;

import android.view.Surface;

import androidx.annotation.NonNull;

import com.wpe.wpeview.WPEView;

import org.mozilla.vrbrowser.browser.api.DisplayAPI;

public class WPEDisplay implements DisplayAPI {

    private WPEView mView;
    WPESession mSession;
    WPEDisplay(WPEView view, WPESession session) {
        mView = view;
        mSession = session;
    }

    @Override
    public void surfaceChanged(@NonNull Surface surface, int width, int height) {
        mView.callSurfaceChanged(surface, width, height);
        mSession.notifyFirstComposite();
    }

    @Override
    public void surfaceChanged(@NonNull Surface surface, int left, int top, int width, int height) {
        mView.callSurfaceChanged(surface, width, height);
        mSession.notifyFirstComposite();
    }

    @Override
    public void surfaceDestroyed() {
        mView.callSurfaceDestroyed();
    }
}
