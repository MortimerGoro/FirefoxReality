package org.mozilla.vrbrowser.browser.api;

import androidx.annotation.AnyThread;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

public interface HistoryDelegate {
    /**
     * A representation of an entry in browser history.
     */
    public interface HistoryItem {
        /**
         * Get the URI of this history element.
         *
         * @return A String representing the URI of this history element.
         */
        @AnyThread
        default @NonNull String getUri() {
            throw new UnsupportedOperationException("HistoryItem.getUri() called on invalid object.");
        }

        /**
         * Get the title of this history element.
         *
         * @return A String representing the title of this history element.
         */
        @AnyThread
        default @NonNull String getTitle() {
            throw new UnsupportedOperationException("HistoryItem.getString() called on invalid object.");
        }
    }

    /**
     * A representation of browser history, accessible as a `List`. The list itself
     * and its entries are immutable; any attempt to mutate will result in an
     * `UnsupportedOperationException`.
     */
    public interface HistoryList extends List<HistoryItem> {
        /**
         * Get the current index in browser history.
         *
         * @return An int representing the current index in browser history.
         */
        @AnyThread
        default int getCurrentIndex() {
            throw new UnsupportedOperationException("HistoryList.getCurrentIndex() called on invalid object.");
        }
    }

    // These flags are similar to those in `IHistory::LoadFlags`, but we use
    // different values to decouple GeckoView from Gecko changes. These
    // should be kept in sync with `GeckoViewHistory::GeckoViewVisitFlags`.

    /** The URL was visited a top-level window. */
    final int VISIT_TOP_LEVEL = 1 << 0;
    /** The URL is the target of a temporary redirect. */
    final int VISIT_REDIRECT_TEMPORARY = 1 << 1;
    /** The URL is the target of a permanent redirect. */
    final int VISIT_REDIRECT_PERMANENT = 1 << 2;
    /** The URL is temporarily redirected to another URL. */
    final int VISIT_REDIRECT_SOURCE = 1 << 3;
    /** The URL is permanently redirected to another URL. */
    final int VISIT_REDIRECT_SOURCE_PERMANENT = 1 << 4;
    /** The URL failed to load due to a client or server error. */
    final int VISIT_UNRECOVERABLE_ERROR = 1 << 5;

    /**
     * Records a visit to a page.
     *
     * @param session The session where the URL was visited.
     * @param url The visited URL.
     * @param lastVisitedURL The last visited URL in this session, to detect
     *                       redirects and reloads.
     * @param flags Additional flags for this visit, including redirect and
     *              error statuses. This is a bitmask of one or more
     *              {@link #VISIT_TOP_LEVEL VISIT_*} flags, OR-ed together.
     * @return A {@link ResultAPI} completed with a boolean indicating
     *         whether to highlight links for the new URL as visited
     *         ({@code true}) or unvisited ({@code false}).
     */
    @UiThread
    default @Nullable
    ResultAPI<Boolean> onVisited(@NonNull final SessionAPI session,
                                 @NonNull final String url,
                                 @Nullable final String lastVisitedURL,
                                 @VisitFlags final int flags) {
        return null;
    }

    /**
     * Returns the visited statuses for links on a page. This is used to
     * highlight links as visited or unvisited, for example.
     *
     * @param session The session requesting the visited statuses.
     * @param urls A list of URLs to check.
     * @return A {@link ResultAPI} completed with a list of booleans
     *         corresponding to the URLs in {@code urls}, and indicating
     *         whether to highlight links for each URL as visited
     *         ({@code true}) or unvisited ({@code false}).
     */
    @UiThread
    default @Nullable
    ResultAPI<boolean[]> getVisited(@NonNull final SessionAPI session,
                                    @NonNull final String[] urls) {
        return null;
    }

    @UiThread
    @SuppressWarnings("checkstyle:javadocmethod")
    default void onHistoryStateChange(@NonNull final SessionAPI session, @NonNull final HistoryList historyList) {}
}

@Retention(RetentionPolicy.SOURCE)
@IntDef(flag = true,
        value = {
                HistoryDelegate.VISIT_TOP_LEVEL,
                HistoryDelegate.VISIT_REDIRECT_TEMPORARY,
                HistoryDelegate.VISIT_REDIRECT_PERMANENT,
                HistoryDelegate.VISIT_REDIRECT_SOURCE,
                HistoryDelegate.VISIT_REDIRECT_SOURCE_PERMANENT,
                HistoryDelegate.VISIT_UNRECOVERABLE_ERROR
        })
        /* package */ @interface VisitFlags {}