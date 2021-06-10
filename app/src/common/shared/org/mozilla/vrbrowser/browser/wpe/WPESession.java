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
import org.mozilla.vrbrowser.ui.widgets.WindowWidget;

import java.util.ArrayList;


public class WPESession extends SessionAPI implements WPEViewClient, WebChromeClient {
    private Context mContext;
    private WPEView mWPEView;
    private RuntimeAPI mRuntime;
    private SessionSettingsAPI mSettings;
    private SessionTextInput mTextInput;
    private WPEDisplay mDisplay;
    private WindowWidget mWindow;

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
        mRuntime = runtime;
        mWPEView = new WPEView(mContext);
        mWPEView.setWPEViewClient(this);
        mWPEView.setWebChromeClient(this);
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
        if (!uri.toLowerCase().startsWith("http")) {
            uri = "https://" + uri;
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
    public void attachToWindow(WindowWidget window)
    {
        if (window == mWindow) {
            return;
        }
        detachFromWindow();
        mWindow = window;
        mWindow.addView(mWPEView);
    }

    @Override
    public void detachFromWindow()
    {
        if (mWindow != null) {
            mWindow.removeView(mWPEView);
            mWindow = null;
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
        mDisplay = new WPEDisplay(mWPEView, this);

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
        com.wpe.wpe.gfx.View view = mWPEView.getView();
        if (view != null) {
            return view.onTouchEvent(event);
        }
        return false;
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


    /* package */ void notifyFirstComposite() {
        mWPEView.postDelayed(() -> {
            if (mContentDelegate != null) {
                mContentDelegate.onFirstComposite(WPESession.this);
                mContentDelegate.onFirstContentfulPaint(WPESession.this);
            }
        }, 0);
    }



    // WPEViewClient
    @Override
    public void onPageStarted(WPEView view, String url) {
        if (mProgressDelegate != null) {
            mProgressDelegate.onPageStart(this, url);
        }
        if (mNavigationDelegate != null) {
            mNavigationDelegate.onLocationChange(this, url);
            mNavigationDelegate.onCanGoBack(this, view.canGoBack());
            mNavigationDelegate.onCanGoForward(this, view.canGoForward());
        }
    }

    @Override
    public void onPageFinished(WPEView view, String url) {
        if (mProgressDelegate != null) {
            mProgressDelegate.onPageStop(this, true);
        }
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
