package org.mozilla.vrbrowser.telemetry;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;


import org.mozilla.vrbrowser.browser.SettingsStore;
import org.mozilla.vrbrowser.search.SearchEngineWrapper;
import org.mozilla.vrbrowser.utils.DeviceType;
import org.mozilla.vrbrowser.utils.SystemUtils;
import org.mozilla.vrbrowser.utils.UrlUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;

import mozilla.components.concept.fetch.Client;


import static org.mozilla.vrbrowser.ui.widgets.Windows.MAX_WINDOWS;
import static org.mozilla.vrbrowser.ui.widgets.Windows.WindowPlacement;


public class GleanMetricsService {

    private final static String APP_NAME = "FirefoxReality";
    private final static String LOGTAG = SystemUtils.createLogtag(GleanMetricsService.class);
    private static boolean initialized = false;
    private static Context context = null;
    private static HashSet<String> domainMap = new HashSet<String>();

    // We should call this at the application initial stage.
    public static void init(@NonNull Context aContext, @NonNull Client client) {
        if (initialized)
            return;

        context = aContext;
        initialized = true;


        setStartupMetrics();
    }

    // It would be called when users turn on/off the setting of telemetry.
    // e.g., SettingsStore.getInstance(context).setTelemetryEnabled();
    public static void start() {

    }

    // It would be called when users turn on/off the setting of telemetry.
    // e.g., SettingsStore.getInstance(context).setTelemetryEnabled();
    public static void stop() {

    }

    public static void startPageLoadTime(String aUrl) {

    }

    public static void stopPageLoadTimeWithURI(String uri) {

    }

    public static void windowsResizeEvent() {

    }

    public static void windowsMoveEvent() {

    }

    public static void activePlacementEvent(int from, boolean active) {

    }

    public static void openWindowsEvent(int from, int to, boolean isPrivate) {

    }

    public static void resetOpenedWindowsCount(int number, boolean isPrivate) {

    }

    public static void sessionStop() {

    }

    @UiThread
    public static void urlBarEvent(boolean aIsUrl) {

    }

    @UiThread
    public static void voiceInputEvent() {

    }

    public static void startImmersive() {

    }

    public static void stopImmersive() {

    }

    public static void openWindowEvent(int windowId) {

    }

    public static void closeWindowEvent(int windowId) {

    }

    private static String getDefaultSearchEngineIdentifierForTelemetry() {
        return SearchEngineWrapper.get(context).getIdentifier();
    }

    public static void newWindowOpenEvent() {

    }

    private static void setStartupMetrics() {

    }

    @VisibleForTesting
    public static void testSetStartupMetrics() {
        setStartupMetrics();
    }

    public static class FxA {

        public static void signIn() {

        }

        public static void signInResult(boolean status) {

        }

        public static void signOut() {

        }

        public static void bookmarksSyncStatus(boolean status) {

        }

        public static void historySyncStatus(boolean status) {

        }

        public static void sentTab() {

        }

        public static void receivedTab(@NonNull mozilla.components.concept.sync.DeviceType source) {

        }
    }

    public static class Tabs {

        public enum TabSource {
            CONTEXT_MENU,       // Tab opened from the browsers long click context menu
            TABS_DIALOG,        // Tab opened from the tabs dialog
            BOOKMARKS,          // Tab opened from the bookmarks panel
            HISTORY,            // Tab opened from the history panel
            DOWNLOADS,          // Tab opened from the downloads panel
            FXA_LOGIN,          // Tab opened by the FxA login flow
            RECEIVED,           // Tab opened by FxA when a tab is received
            PRE_EXISTING,       // Tab opened as a result of restoring the last session
            BROWSER,            // Tab opened by the browser as a result of a new window open
        }

        public static void openedCounter(@NonNull TabSource source) {

        }

        public static void activatedEvent() {

        }
    }
}
