package org.mozilla.vrbrowser.browser.wpe;

import android.content.Context;
import android.graphics.Matrix;
import android.view.MotionEvent;
import android.view.View;

import com.wpe.wpeview.WPEView;
import com.wpe.wpeview.WPEViewClient;
import com.wpe.wpeview.WebChromeClient;

import org.mozilla.vrbrowser.browser.api.DisplayAPI;
import org.mozilla.vrbrowser.browser.api.RuntimeAPI;
import org.mozilla.vrbrowser.browser.api.SessionAPI;
import org.mozilla.vrbrowser.browser.api.SessionSettingsAPI;
import org.mozilla.vrbrowser.browser.api.SessionStateAPI;
import org.mozilla.vrbrowser.browser.api.SessionTextInput;
import org.mozilla.vrbrowser.browser.engine.SessionStore;

import java.util.Timer;
import java.util.TimerTask;


public class WPESession extends SessionAPI implements WPEViewClient, WebChromeClient {
    private Context mContext;
    private WPEView mWPEView;
    private RuntimeAPI mRuntime;
    private SessionSettingsAPI mSettings;
    private SessionTextInput mTextInput;
    private WPEDisplay mDisplay;
    private boolean mFirstCompositeNotified = false;

    public WPESession(Context context) {
        mContext = context;
        mSettings = new SessionSettingsAPI();
        mTextInput = new SessionTextInput(this);
    }

    @Override
    public boolean isOpen() {
        return mWPEView != null;
    }

    @Override
    public void open(RuntimeAPI runtime) {
        mWPEView = new WPEView(mContext);
        mWPEView.setWPEViewClient(this);
        mWPEView.setWebChromeClient(this);
        mRuntime = runtime;
    }

    @Override
    public void setActive(boolean active) {

    }

    @Override
    public void stop() {
        if (!isOpen()) {
            throw new RuntimeException("WPE Session is not opened");
        }
        mWPEView.stopLoading();
    }

    @Override
    public void close() {
        if (!isOpen()) {
            throw new RuntimeException("WPE Session is not opened");
        }
        stop();
        mWPEView.setWebChromeClient(null);
        mWPEView.setWPEViewClient(null);
        mWPEView = null;
    }

    @Override
    public void loadUri(String uri, int flags) {
        if (!isOpen()) {
            throw new RuntimeException("WPE Session is not opened");
        }
        mWPEView.loadUrl(uri);
    }

    @Override
    public void loadData(byte[] data, String contentType) {
        if (!isOpen()) {
            throw new RuntimeException("WPE Session is not opened");
        }
    }

    @Override
    public void reload(int flags) {
        if (!isOpen()) {
            throw new RuntimeException("WPE Session is not opened");
        }
        mWPEView.reload();
    }

    @Override
    public void goBack() {
        if (!isOpen()) {
            throw new RuntimeException("WPE Session is not opened");
        }
        mWPEView.goBack();
    }

    @Override
    public void goForward() {
        if (!isOpen()) {
            throw new RuntimeException("WPE Session is not opened");
        }
        mWPEView.goForward();

    }

    @Override
    public void purgeHistory() {
        if (!isOpen()) {
            throw new RuntimeException("WPE Session is not opened");
        }
    }

    @Override
    public void exitFullScreen() {
        if (!isOpen()) {
            throw new RuntimeException("WPE Session is not opened");
        }
    }

    @Override
    public void restoreState(SessionStateAPI state) {
        if (!isOpen()) {
            throw new RuntimeException("WPE Session is not opened");
        }
        if (!state.isEmpty()) {
            mWPEView.loadUrl(state.get(state.getCurrentIndex()).getUri());
        }
    }

    @Override
    public DisplayAPI acquireDisplay() {
        if (!isOpen()) {
            throw new RuntimeException("WPE Session is not opened");
        }
        if (mDisplay != null) {
            throw new RuntimeException("Display already acquired");
        }
        mDisplay = new WPEDisplay(mWPEView);
        return mDisplay;
    }

    @Override
    public void releaseDisplay(DisplayAPI display) {
        if (!isOpen()) {
            throw new RuntimeException("WPE Session is not opened");
        }
        if (mDisplay == null || mDisplay != display) {
            throw new RuntimeException("Invalid display");
        }
        mDisplay = null;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isOpen()) {
            throw new RuntimeException("WPE Session is not opened");
        }
        return mWPEView.onTouchEvent(event);
    }

    @Override
    public boolean onMotionEvent(MotionEvent event) {
        if (!isOpen()) {
            throw new RuntimeException("WPE Session is not opened");
        }
        return mWPEView.onGenericMotionEvent(event);
    }

    @Override
    public SessionSettingsAPI getSettings() {
        return mSettings;
    }

    @Override
    public SessionTextInput getTextInput() {
        if (!isOpen()) {
            throw new RuntimeException("WPE Session is not opened");
        }
        return mTextInput;
    }

    @Override
    public void getClientToSurfaceMatrix(Matrix matrix) {
        if (!isOpen()) {
            throw new RuntimeException("WPE Session is not opened");
        }
        matrix.reset();
    }

    @Override
    public View getView() {
        return mWPEView;
    }

    // WPEViewClient
    @Override
    public void onPageStarted(WPEView view, String url) {
        if (mNavigationDelegate != null) {
            mNavigationDelegate.onLocationChange(this, url);
            mNavigationDelegate.onCanGoBack(this, view.canGoBack());
            mNavigationDelegate.onCanGoForward(this, view.canGoForward());
        }
        if (!mFirstCompositeNotified) {
            mFirstCompositeNotified = true;
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    if (mContentDelegate != null) {
                        mContentDelegate.onFirstComposite(WPESession.this);
                        mContentDelegate.onFirstContentfulPaint(WPESession.this);
                    }
                }
            }, 200);
        }
    }

    @Override
    public void onPageFinished(WPEView view, String url) {
        if (mNavigationDelegate != null) {
            mNavigationDelegate.onLocationChange(this, url);
            mNavigationDelegate.onCanGoBack(this, view.canGoBack());
            mNavigationDelegate.onCanGoForward(this, view.canGoForward());
        }
    }

    // WebChromeClient

    @Override
    public void onProgressChanged(WPEView view, int progress) {
        if (mProgressDelegate != null) {
            mProgressDelegate.onProgressChange(this, progress);
        }
    }

    @Override
    public void onReceivedTitle(WPEView view, String title) {
        if (mContentDelegate != null) {
            mContentDelegate.onTitleChange(this, title);
        }
    }
}
