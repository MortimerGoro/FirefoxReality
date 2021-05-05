package org.mozilla.vrbrowser.browser.api;

import android.view.Surface;

import androidx.annotation.NonNull;

public interface DisplayAPI {
    void surfaceChanged(@NonNull final Surface surface, final int width, final int height);
    void surfaceChanged(@NonNull final Surface surface, final int left, final int top, final int width, final int height);
    void surfaceDestroyed();
}
