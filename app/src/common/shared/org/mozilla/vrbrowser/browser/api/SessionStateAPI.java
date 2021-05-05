package org.mozilla.vrbrowser.browser.api;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.AbstractSequentialList;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * Class representing a saved session state.
 */
@AnyThread
public class SessionStateAPI extends AbstractSequentialList<HistoryDelegate.HistoryItem>
        implements HistoryDelegate.HistoryList, Parcelable {
    private static final String LOGTAG = "SessionState";
    private BundleAPI mState;

    private class SessionStateItem implements HistoryDelegate.HistoryItem {
        private final BundleAPI mItem;

        private SessionStateItem(final @NonNull BundleAPI item) {
            mItem = item;
        }

        @Override /* HistoryItem */
        public String getUri() {
            return mItem.getString("url");
        }

        @Override /* HistoryItem */
        public String getTitle() {
            return mItem.getString("title");
        }
    }

    private class SessionStateIterator implements ListIterator<HistoryDelegate.HistoryItem> {
        private final SessionStateAPI mState;
        private int mIndex;

        private SessionStateIterator(final @NonNull SessionStateAPI state) {
            this(state, 0);
        }

        private SessionStateIterator(final @NonNull SessionStateAPI state, final int index) {
            mIndex = index;
            mState = state;
        }

        @Override /* ListIterator */
        public void add(final HistoryDelegate.HistoryItem item) {
            throw new UnsupportedOperationException();
        }

        @Override /* ListIterator */
        public boolean hasNext() {
            final BundleAPI[] entries = mState.getHistoryEntries();

            if (entries == null) {
                Log.w(LOGTAG, "No history entries found.");
                return false;
            }

            if (mIndex >= mState.getHistoryEntries().length) {
                return false;
            }
            return true;
        }

        @Override /* ListIterator */
        public boolean hasPrevious() {
            if (mIndex <= 0) {
                return false;
            }
            return true;
        }

        @Override /* ListIterator */
        public HistoryDelegate.HistoryItem next() {
            if (hasNext()) {
                mIndex++;
                return new SessionStateItem(mState.getHistoryEntries()[mIndex - 1]);
            } else {
                throw new NoSuchElementException();
            }
        }

        @Override /* ListIterator */
        public int nextIndex() {
            return mIndex;
        }

        @Override /* ListIterator */
        public HistoryDelegate.HistoryItem previous() {
            if (hasPrevious()) {
                mIndex--;
                return new SessionStateItem(mState.getHistoryEntries()[mIndex]);
            } else {
                throw new NoSuchElementException();
            }
        }

        @Override /* ListIterator */
        public int previousIndex() {
            return mIndex - 1;
        }

        @Override /* ListIterator */
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override /* ListIterator */
        public void set(final @NonNull HistoryDelegate.HistoryItem item) {
            throw new UnsupportedOperationException();
        }
    }

    private SessionStateAPI() {
        mState = new BundleAPI(3);
    }

    private SessionStateAPI(final @NonNull BundleAPI state) {
        mState = new BundleAPI(state);
    }

    @SuppressWarnings("checkstyle:javadocmethod")
    public SessionStateAPI(final @NonNull SessionStateAPI state) {
        mState = new BundleAPI(state.mState);
    }

    /* package */ void updateSessionState(final @NonNull BundleAPI updateData) {
        if (updateData == null) {
            Log.w(LOGTAG, "Session state update has no data field.");
            return;
        }

        final BundleAPI history = updateData.getBundle("historychange");
        final BundleAPI scroll = updateData.getBundle("scroll");
        final BundleAPI formdata = updateData.getBundle("formdata");

        if (history != null) {
            mState.putBundle("history", history);
        }

        if (scroll != null) {
            mState.putBundle("scrolldata", scroll);
        }

        if (formdata != null) {
            mState.putBundle("formdata", formdata);
        }

        return;
    }

    @Override
    public int hashCode() {
        return mState.hashCode();
    }

    @Override
    public boolean equals(final Object other) {
        if (other == null || !(other instanceof SessionStateAPI)) {
            return false;
        }

        final SessionStateAPI otherState = (SessionStateAPI)other;

        return this.mState.equals(otherState.mState);
    }

    /**
     * Creates a new SessionState instance from a value previously returned by
     * {@link #toString()}.
     *
     * @param value The serialized SessionState in String form.
     * @return A new SessionState instance if input is valid; otherwise null.
     */
    public static @Nullable
    SessionStateAPI fromString(final @Nullable String value) {
        final BundleAPI bundleState;
        try {
            bundleState = BundleAPI.fromJSONObject(new JSONObject(value));
        } catch (final Exception e) {
            Log.e(LOGTAG, "String does not represent valid session state.");
            return null;
        }

        if (bundleState == null) {
            return null;
        }

        return new SessionStateAPI(bundleState);
    }

    @Override
    public @Nullable String toString() {
        if (mState == null) {
            Log.w(LOGTAG, "Can't convert SessionState with null state to string");
            return null;
        }

        String res;
        try {
            res = mState.toJSONObject().toString();
        } catch (final JSONException e) {
            Log.e(LOGTAG, "Could not convert session state to string.");
            res = null;
        }

        return res;
    }

    @Override // Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // Parcelable
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeString(toString());
    }

    // AIDL code may call readFromParcel even though it's not part of Parcelable.
    @SuppressWarnings("checkstyle:javadocmethod")
    public void readFromParcel(final @NonNull Parcel source) {
        if (source.readString() == null) {
            Log.w(LOGTAG, "Can't reproduce session state from Parcel");
        }

        try {
            mState = BundleAPI.fromJSONObject(new JSONObject(source.readString()));
        } catch (final JSONException e) {
            Log.e(LOGTAG, "Could not convert string to session state.");
            mState = null;
        }
    }

    public static final Parcelable.Creator<SessionStateAPI> CREATOR =
            new Parcelable.Creator<SessionStateAPI>() {
                @Override
                public SessionStateAPI createFromParcel(final Parcel source) {
                    if (source.readString() == null) {
                        Log.w(LOGTAG, "Can't create session state from Parcel");
                    }

                    BundleAPI res;
                    try {
                        res = BundleAPI.fromJSONObject(new JSONObject(source.readString()));
                    } catch (final JSONException e) {
                        Log.e(LOGTAG, "Could not convert parcel to session state.");
                        res = null;
                    }

                    return new SessionStateAPI(res);
                }

                @Override
                public SessionStateAPI[] newArray(final int size) {
                    return new SessionStateAPI[size];
                }
            };

    @Override /* AbstractSequentialList */
    public @NonNull HistoryDelegate.HistoryItem get(final int index) {
        final BundleAPI[] entries = getHistoryEntries();

        if (entries == null || index < 0 || index >= entries.length) {
            throw new NoSuchElementException();
        }

        return new SessionStateItem(entries[index]);
    }

    @Override /* AbstractSequentialList */
    public @NonNull
    Iterator<HistoryDelegate.HistoryItem> iterator() {
        return listIterator(0);
    }

    @Override /* AbstractSequentialList */
    public @NonNull ListIterator<HistoryDelegate.HistoryItem> listIterator(final int index) {
        return new SessionStateIterator(this, index);
    }

    @Override /* AbstractSequentialList */
    public int size() {
        final BundleAPI[] entries = getHistoryEntries();

        if (entries == null) {
            Log.w(LOGTAG, "No history entries found.");
            return 0;
        }

        return entries.length;
    }

    @Override /* HistoryList */
    public int getCurrentIndex() {
        final BundleAPI history = getHistory();

        if (history == null) {
            throw new IllegalStateException("No history state exists.");
        }

        return history.getInt("index") + history.getInt("fromIdx");
    }

    // Some helpers for common code.
    private BundleAPI getHistory() {
        if (mState == null) {
            return null;
        }

        return mState.getBundle("history");
    }

    private BundleAPI[] getHistoryEntries() {
        final BundleAPI history = getHistory();

        if (history == null) {
            return null;
        }

        return history.getBundleArray("entries");
    }
}
