package org.mozilla.vrbrowser.browser.api;

import android.content.Context;
import android.widget.FrameLayout;

public class RuntimeAPI {
    private Context mContext;
    private FrameLayout mWidgetContainer;

    public RuntimeAPI(Context context) {
        mContext = context;
    }

    public Context getContext() {
        return  mContext;
    }
    public void setDefaultPrefs(BundleAPI bundle) {

    }

    public void setWidgetContainer(FrameLayout view) {
        mWidgetContainer = view;
    }

    public FrameLayout getWidgetContainer() {
        return mWidgetContainer;
    }
}
