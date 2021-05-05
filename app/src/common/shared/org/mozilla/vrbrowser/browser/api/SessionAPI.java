package org.mozilla.vrbrowser.browser.api;


import android.graphics.Matrix;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.IntDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public abstract class SessionAPI {
    protected NavigationDelegate mNavigationDelegate;
    protected ProgressDelegate mProgressDelegate;
    protected ContentDelegate mContentDelegate;
    protected TextInputDelegate mTextInputDelegate;
    protected PermissionDelegate mPermissionDelegate;
    protected PromptDelegate mPromptDelegate;
    protected ContentBlocking.Delegate mContentBlockingDelegate;
    protected MediaDelegate mMediaDelegate;
    protected HistoryDelegate mHistoryDelegate;
    protected SelectionActionDelegate mSelectionActionDelegate;


    public abstract boolean isOpen();
    public abstract void open(RuntimeAPI runtime);
    public abstract void setActive(boolean active);
    public abstract void stop();
    public abstract void close();
    public abstract void loadUri(String uri, @LoadFlags int flags);
    public void loadUri(String uri) {
        loadUri(uri, LOAD_FLAGS_NONE);
    }
    public abstract void loadData(byte[] data, String contentType);
    public abstract void reload(@LoadFlags int flags);
    public abstract void goBack();
    public abstract void goForward();
    public abstract void purgeHistory();
    public abstract void exitFullScreen();
    public abstract void restoreState(SessionStateAPI state);

    public abstract DisplayAPI acquireDisplay();
    public abstract void releaseDisplay(DisplayAPI display);

    public abstract boolean onTouchEvent(MotionEvent event);
    public abstract boolean onMotionEvent(MotionEvent event);

    public abstract SessionSettingsAPI getSettings();
    public abstract SessionTextInput getTextInput();
    public abstract void getClientToSurfaceMatrix(Matrix matrix);
    public abstract View getView();

    public void setNavigationDelegate(NavigationDelegate delegate) {
        mNavigationDelegate = delegate;
    }
    public void setProgressDelegate(ProgressDelegate delegate) {
        mProgressDelegate = delegate;
    }

    public void setContentDelegate(ContentDelegate delegate) {
        mContentDelegate = delegate;
    }

    public void setTextInputDelegate(TextInputDelegate delegate) {
        mTextInputDelegate = delegate;
    }

    public void setPermissionDelegate(PermissionDelegate delegate) {
        mPermissionDelegate = delegate;
    }

    public void setPromptDelegate(PromptDelegate delegate) {
        mPromptDelegate = delegate;
    }

    public void setContentBlockingDelegate(ContentBlocking.Delegate delegate) {
        mContentBlockingDelegate = delegate;
    }

    public void setMediaDelegate(MediaDelegate delegate) {
        mMediaDelegate = delegate;
    }

    public void setHistoryDelegate(HistoryDelegate delegate) {
        mHistoryDelegate = delegate;
    }

    public void setSelectionActionDelegate(SelectionActionDelegate delegate) {
        mSelectionActionDelegate = delegate;
    }

    /**
     * Default load flag, no special considerations.
     */
    public static final int LOAD_FLAGS_NONE = 0;
    /**
     * Bypass the cache.
     */
    public static final int LOAD_FLAGS_BYPASS_CACHE = 1 << 0;

    /**
     * Bypass the proxy, if one has been configured.
     */
    public static final int LOAD_FLAGS_BYPASS_PROXY = 1 << 1;

    /**
     * The load is coming from an external app. Perform additional checks.
     */
    public static final int LOAD_FLAGS_EXTERNAL = 1 << 2;

    /**
     * Popup blocking will be disabled for this load
     */
    public static final int LOAD_FLAGS_ALLOW_POPUPS = 1 << 3;

    /**
     * Bypass the URI classifier (content blocking and Safe Browsing).
     */
    public static final int LOAD_FLAGS_BYPASS_CLASSIFIER = 1 << 4;

    /**
     * Allows a top-level data: navigation to occur. E.g. view-image
     * is an explicit user action which should be allowed.
     */
    public static final int LOAD_FLAGS_FORCE_ALLOW_DATA_URI = 1 << 5;

    /**
     * This flag specifies that any existing history entry should be replaced.
     */
    public static final int LOAD_FLAGS_REPLACE_HISTORY = 1 << 6;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(flag = true,
            value = { LOAD_FLAGS_NONE, LOAD_FLAGS_BYPASS_CACHE, LOAD_FLAGS_BYPASS_PROXY,
                    LOAD_FLAGS_EXTERNAL, LOAD_FLAGS_ALLOW_POPUPS, LOAD_FLAGS_FORCE_ALLOW_DATA_URI, LOAD_FLAGS_REPLACE_HISTORY })
            /* package */ @interface LoadFlags {}

}
