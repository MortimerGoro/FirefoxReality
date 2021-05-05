package org.mozilla.vrbrowser.browser.api;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import org.mozilla.vrbrowser.browser.engine.Session;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public interface NavigationDelegate {
    /**
     * A view has started loading content from the network.
     * @param session The Session that initiated the callback.
     * @param url The resource being loaded.
     */
    @UiThread
    default void onLocationChange(@NonNull final SessionAPI session, @Nullable final String url) {}

    /**
     * The view's ability to go back has changed.
     * @param session The Session that initiated the callback.
     * @param canGoBack The new value for the ability.
     */
    @UiThread
    default void onCanGoBack(@NonNull final SessionAPI session, final boolean canGoBack) {}

    /**
     * The view's ability to go forward has changed.
     * @param session The Session that initiated the callback.
     * @param canGoForward The new value for the ability.
     */
    @UiThread
    default void onCanGoForward(@NonNull final SessionAPI session, final boolean canGoForward) {}

    public static final int TARGET_WINDOW_NONE = 0;
    public static final int TARGET_WINDOW_CURRENT = 1;
    public static final int TARGET_WINDOW_NEW = 2;

    // Match with nsIWebNavigation.idl.
    /**
     * The load request was triggered by an HTTP redirect.
     */
    static final int LOAD_REQUEST_IS_REDIRECT = 0x800000;

    /**
     * Load request details.
     */
    public static class LoadRequest {
        /* package */ LoadRequest(@NonNull final String uri,
                                  @Nullable final String triggerUri,
                                  final int geckoTarget,
                                  final int flags,
                                  final boolean hasUserGesture,
                                  final boolean isDirectNavigation) {
            this.uri = uri;
            this.triggerUri = triggerUri;
            this.target = convertGeckoTarget(geckoTarget);
            this.isRedirect = (flags & LOAD_REQUEST_IS_REDIRECT) != 0;
            this.hasUserGesture = hasUserGesture;
            this.isDirectNavigation = isDirectNavigation;
        }

        /**
         * Empty constructor for tests.
         */
        protected LoadRequest() {
            uri = "";
            triggerUri = null;
            target = 0;
            isRedirect = false;
            hasUserGesture = false;
            isDirectNavigation = false;
        }

        // This needs to match nsIBrowserDOMWindow.idl
        private @TargetWindow int convertGeckoTarget(final int geckoTarget) {
            switch (geckoTarget) {
                case 0: // OPEN_DEFAULTWINDOW
                case 1: // OPEN_CURRENTWINDOW
                    return TARGET_WINDOW_CURRENT;
                default: // OPEN_NEWWINDOW, OPEN_NEWTAB
                    return TARGET_WINDOW_NEW;
            }
        }

        /**
         * The URI to be loaded.
         */
        public final @NonNull String uri;

        /**
         * The URI of the origin page that triggered the load request.
         * null for initial loads and loads originating from data: URIs.
         */
        public final @Nullable String triggerUri;

        /**
         * The target where the window has requested to open.
         * One of {@link #TARGET_WINDOW_NONE TARGET_WINDOW_*}.
         */
        public final @TargetWindow int target;

        /**
         * True if and only if the request was triggered by an HTTP redirect.
         *
         * If the user loads URI "a", which redirects to URI "b", then
         * <code>onLoadRequest</code> will be called twice, first with uri "a" and
         * <code>isRedirect = false</code>, then with uri "b" and
         * <code>isRedirect = true</code>.
         */
        public final boolean isRedirect;

        /**
         * True if there was an active user gesture when the load was requested.
         */
        public final boolean hasUserGesture;

        /**
         * This load request was initiated by a direct navigation from the
         * application. E.g. when calling {@link SessionAPI#load}.
         */
        public final boolean isDirectNavigation;

        @Override
        public String toString() {
            final StringBuilder out = new StringBuilder("LoadRequest { ");
            out
                    .append("uri: " + uri)
                    .append(", triggerUri: " + triggerUri)
                    .append(", target: " + target)
                    .append(", isRedirect: " + isRedirect)
                    .append(", hasUserGesture: " + hasUserGesture)
                    .append(", fromLoadUri: " + hasUserGesture)
                    .append(" }");
            return out.toString();
        }
    }

    /**
     * A request to open an URI. This is called before each top-level page load to
     * allow custom behavior.
     * For example, this can be used to override the behavior of
     * TAGET_WINDOW_NEW requests, which defaults to requesting a new
     * Session via onNewSession.
     *
     * @param session The Session that initiated the callback.
     * @param request The {@link LoadRequest} containing the request details.
     *
     * @return A {@link ResultAPI} with a {@link AllowOrDeny} value which indicates whether
     *         or not the load was handled. If unhandled, Gecko will continue the
     *         load as normal. If handled (a {@link AllowOrDeny#DENY DENY} value), Gecko
     *         will abandon the load. A null return value is interpreted as
     *         {@link AllowOrDeny#ALLOW ALLOW} (unhandled).
     */
    @UiThread
    default @Nullable
    ResultAPI<AllowOrDeny> onLoadRequest(@NonNull final SessionAPI session,
                                         @NonNull final LoadRequest request) {
        return null;
    }

    /**
     * A request to load a URI in a non-top-level context.
     *
     * @param session The Session that initiated the callback.
     * @param request The {@link LoadRequest} containing the request details.
     *
     * @return A {@link ResultAPI} with a {@link AllowOrDeny} value which indicates whether
     *         or not the load was handled. If unhandled, Gecko will continue the
     *         load as normal. If handled (a {@link AllowOrDeny#DENY DENY} value), Gecko
     *         will abandon the load. A null return value is interpreted as
     *         {@link AllowOrDeny#ALLOW ALLOW} (unhandled).
     */
    @UiThread
    default @Nullable
    ResultAPI<AllowOrDeny> onSubframeLoadRequest(@NonNull final SessionAPI session,
                                                 @NonNull final LoadRequest request) {
        return null;
    }

    /**
     * A request has been made to open a new session. The URI is provided only for
     * informational purposes. Do not call Session.load here. Additionally, the
     * returned Session must be a newly-created one.
     *
     * @param session The Session that initiated the callback.
     * @param uri The URI to be loaded.
     *
     * @return A {@link ResultAPI} which holds the returned Session. May be null, in
     *        which case the request for a new window by web content will fail. e.g.,
     *        <code>window.open()</code> will return null.
     *        The implementation of onNewSession is responsible for maintaining a reference
     *        to the returned object, to prevent it from being garbage collected.
     */
    @UiThread
    default @Nullable
    ResultAPI<SessionAPI> onNewSession(@NonNull final SessionAPI session,
                                    @NonNull final String uri) {
        return null;
    }

    /**
     * @param session The Session that initiated the callback.
     * @param uri The URI that failed to load.
     * @param error A WebRequestError containing details about the error
     * @return A URI to display as an error. Returning null will halt the load entirely.
     *         The following special methods are made available to the URI:
     *         - document.addCertException(isTemporary), returns Promise
     *         - document.getFailedCertSecurityInfo(), returns FailedCertSecurityInfo
     *         - document.getNetErrorInfo(), returns NetErrorInfo
     *         - document.allowDeprecatedTls, a property indicating whether or not TLS 1.0/1.1 is allowed
     * @see <a href="https://searchfox.org/mozilla-central/source/dom/webidl/FailedCertSecurityInfo.webidl">FailedCertSecurityInfo IDL</a>
     * @see <a href="https://searchfox.org/mozilla-central/source/dom/webidl/NetErrorInfo.webidl">NetErrorInfo IDL</a>
     */
    @UiThread
    default @Nullable
    ResultAPI<String> onLoadError(@NonNull final SessionAPI session,
                                  @Nullable final String uri,
                                  @NonNull final WebRequestError error) {
        return null;
    }
}

@Retention(RetentionPolicy.SOURCE)
@IntDef({NavigationDelegate.TARGET_WINDOW_NONE, NavigationDelegate.TARGET_WINDOW_CURRENT,
        NavigationDelegate.TARGET_WINDOW_NEW})
        /* package */ @interface TargetWindow {}