package org.mozilla.vrbrowser.browser.api;

import android.os.Bundle;

import androidx.annotation.AnyThread;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import org.json.JSONObject;
import org.mozilla.vrbrowser.browser.engine.Session;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public interface ContentDelegate {
    /**
     * A page title was discovered in the content or updated after the content
     * loaded.
     * @param session The Session that initiated the callback.
     * @param title The title sent from the content.
     */
    @UiThread
    default void onTitleChange(@NonNull final SessionAPI session, @Nullable final String title) {}

    /**
     * A page has requested focus. Note that window.focus() in content will not result
     * in this being called.
     * @param session The Session that initiated the callback.
     */
    @UiThread
    default void onFocusRequest(@NonNull final SessionAPI session) {}

    /**
     * A page has requested to close
     * @param session The Session that initiated the callback.
     */
    @UiThread
    default void onCloseRequest(@NonNull final SessionAPI session) {}

    /**
     * A page has entered or exited full screen mode. Typically, the implementation
     * would set the Activity containing the Session to full screen when the page is
     * in full screen mode.
     *
     * @param session The Session that initiated the callback.
     * @param fullScreen True if the page is in full screen mode.
     */
    @UiThread
    default void onFullScreen(@NonNull final SessionAPI session, final boolean fullScreen) {}

    /**
     * A viewport-fit was discovered in the content or updated after the content.
     *
     * @param session The Session that initiated the callback.
     * @param viewportFit The value of viewport-fit of meta element in content.
     * @see <a href="https://drafts.csswg.org/css-round-display/#viewport-fit-descriptor">4.1. The viewport-fit descriptor</a>
     */
    @UiThread
    default void onMetaViewportFitChange(@NonNull final SessionAPI session, @NonNull final String viewportFit) {}

    /**
     * Element details for onContextMenu callbacks.
     */
    public static class ContextElement {
        @Retention(RetentionPolicy.SOURCE)
        @IntDef({TYPE_NONE, TYPE_IMAGE, TYPE_VIDEO, TYPE_AUDIO})
                /* package */ @interface Type {}
        public static final int TYPE_NONE = 0;
        public static final int TYPE_IMAGE = 1;
        public static final int TYPE_VIDEO = 2;
        public static final int TYPE_AUDIO = 3;

        /**
         * The base URI of the element's document.
         */
        public final @Nullable String baseUri;

        /**
         * The absolute link URI (href) of the element.
         */
        public final @Nullable String linkUri;

        /**
         * The title text of the element.
         */
        public final @Nullable String title;

        /**
         * The alternative text (alt) for the element.
         */
        public final @Nullable String altText;

        /**
         * The type of the element.
         * One of the {@link ContextElement#TYPE_NONE} flags.
         */
        public final @Type int type;

        /**
         * The source URI (src) of the element.
         * Set for (nested) media elements.
         */
        public final @Nullable String srcUri;


        protected ContextElement(
                final @Nullable String baseUri,
                final @Nullable String linkUri,
                final @Nullable String title,
                final @Nullable String altText,
                final @NonNull String typeStr,
                final @Nullable String srcUri) {
            this.baseUri = baseUri;
            this.linkUri = linkUri;
            this.title = title;
            this.altText = altText;
            this.type = getType(typeStr);
            this.srcUri = srcUri;
        }

        private static int getType(final String name) {
            if ("HTMLImageElement".equals(name)) {
                return TYPE_IMAGE;
            } else if ("HTMLVideoElement".equals(name)) {
                return TYPE_VIDEO;
            } else if ("HTMLAudioElement".equals(name)) {
                return TYPE_AUDIO;
            }
            return TYPE_NONE;
        }
    }

    @AnyThread
    static public class WebResponseInfo {
        /**
         * The URI of the response. Cannot be null.
         */
        @NonNull public final String uri;

        /**
         * The content type (mime type) of the response. May be null.
         */
        @Nullable public final String contentType;

        /**
         * The content length of the response. May be 0 if unknokwn.
         */
        @Nullable public final long contentLength;

        /**
         * The filename obtained from the content disposition, if any.
         * May be null.
         */
        @Nullable public final String filename;

        /* package */ WebResponseInfo(final Bundle message) {
            uri = message.getString("uri");
            if (uri == null) {
                throw new IllegalArgumentException("URI cannot be null");
            }

            contentType = message.getString("contentType");
            contentLength = message.getLong("contentLength");
            filename = message.getString("filename");
        }

        /**
         * Empty constructor for tests.
         */
        protected WebResponseInfo() {
            uri = "";
            contentType = "";
            contentLength = 0;
            filename = "";
        }
    }

    /**
     * A user has initiated the context menu via long-press.
     * This event is fired on links, (nested) images and (nested) media
     * elements.
     *
     * @param session The Session that initiated the callback.
     * @param screenX The screen coordinates of the press.
     * @param screenY The screen coordinates of the press.
     * @param element The details for the pressed element.
     */
    @UiThread
    default void onContextMenu(@NonNull final SessionAPI session,
                               final int screenX, final int screenY,
                               @NonNull final ContextElement element) {}

    /**
     * This is fired when there is a response that cannot be handled
     * by Gecko (e.g., a download).
     *  @param session the Session that received the external response.
     * @param response the external WebResponse.
     */
    @UiThread
    default void onExternalResponse(@NonNull final SessionAPI session,
                                    @NonNull final WebResponseInfo response) {}

    /**
     * The content process hosting this Session has crashed. The
     * Session is now closed and unusable. You may call
     * {@link #open(GeckoRuntime)} to recover the session, but no state
     * is preserved. Most applications will want to call
     * {@link #load} or {@link #restoreState(SessionStateAPI)} at this point.
     *
     * @param session The Session for which the content process has crashed.
     */
    @UiThread
    default void onCrash(@NonNull final SessionAPI session) {}

    /**
     * The content process hosting this Session has been killed. The
     * Session is now closed and unusable. You may call
     * {@link #open(GeckoRuntime)} to recover the session, but no state
     * is preserved. Most applications will want to call
     * {@link #load} or {@link #restoreState(SessionStateAPI)} at this point.
     *
     * @param session The Session for which the content process has been killed.
     */
    @UiThread
    default void onKill(@NonNull final SessionAPI session) {}


    /**
     * Notification that the first content composition has occurred.
     * This callback is invoked for the first content composite after either
     * a start or a restart of the compositor.
     * @param session The Session that had a first paint event.
     */
    @UiThread
    default void onFirstComposite(@NonNull final SessionAPI session) {}

    /**
     * Notification that the first content paint has occurred.
     * This callback is invoked for the first content paint after
     * a page has been loaded, or after a {@link #onPaintStatusReset(Session)}
     * event. The function {@link #onFirstComposite(Session)} will be called
     * once the compositor has started rendering. However, it is possible for the
     * compositor to start rendering before there is any content to render.
     * onFirstContentfulPaint() is called once some content has been rendered. It may be nothing
     * more than the page background color. It is not an indication that the whole page has
     * been rendered.
     * @param session The Session that had a first paint event.
     */
    @UiThread
    default void onFirstContentfulPaint(@NonNull final SessionAPI session) {}

    /**
     * Notification that the paint status has been reset.
     *
     * This callback is invoked whenever the painted content is no longer being
     * displayed. This can occur in response to the session being paused.
     * After this has fired the compositor may continue rendering, but may not
     * render the page content. This callback can therefore be used in conjunction
     * with {@link #onFirstContentfulPaint(Session)} to determine when there is
     * valid content being rendered.
     *
     * @param session The Session that had the paint status reset event.
     */
    @UiThread
    default void onPaintStatusReset(@NonNull final SessionAPI session) {}

    /**
     * This is fired when the loaded document has a valid Web App Manifest present.
     *
     * The various colors (theme_color, background_color, etc.) present in the manifest
     * have been  transformed into #AARRGGBB format.
     *
     * @param session The Session that contains the Web App Manifest
     * @param manifest A parsed and validated {@link JSONObject} containing the manifest contents.
     * @see <a href="https://www.w3.org/TR/appmanifest/">Web App Manifest specification</a>
     */
    @UiThread
    default void onWebAppManifest(@NonNull final SessionAPI session, @NonNull final JSONObject manifest) {}

    /**
     * A script has exceeded it's execution timeout value
     * @param geckoSession Session that initiated the callback.
     * @param scriptFileName Filename of the slow script
     * @return A {@link ResultAPI} with a SlowScriptResponse value which indicates whether to
     *         allow the Slow Script to continue processing. Stop will halt the slow script.
     *         Continue will pause notifications for a period of time before resuming.
     */
    @UiThread
    default @Nullable
    ResultAPI<SlowScriptResponse> onSlowScript(@NonNull final SessionAPI session,
                                               @NonNull final String scriptFileName) {
        return null;
    }
}