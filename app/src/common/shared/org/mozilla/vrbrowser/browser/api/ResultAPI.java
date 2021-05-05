package org.mozilla.vrbrowser.browser.api;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.SimpleArrayMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeoutException;


@AnyThread
public class ResultAPI<T> {
    private static final String LOGTAG = "WPEResult";

    private interface Dispatcher {
        void dispatch(Runnable r);
    }

    private static class HandlerDispatcher implements Dispatcher {
        HandlerDispatcher(final Handler h) {
            mHandler = h;
        }
        public void dispatch(final Runnable r) {
            mHandler.post(r);
        }
        @Override
        public boolean equals(final Object other) {
            if (!(other instanceof HandlerDispatcher)) {
                return false;
            }
            return mHandler.equals(((HandlerDispatcher)other).mHandler);
        }
        @Override
        public int hashCode() {
            return mHandler.hashCode();
        }

        Handler mHandler;
    }

    private static class XPCOMEventTargetDispatcher implements Dispatcher {
        public XPCOMEventTargetDispatcher() {
        }

        @Override
        public void dispatch(final Runnable r) {
            r.run();
        }
    }

    private static class DirectDispatcher implements Dispatcher {
        public void dispatch(final Runnable r) {
            r.run();
        }
        static DirectDispatcher sInstance = new DirectDispatcher();
        private DirectDispatcher() {}

    }

    public static final class UncaughtException extends RuntimeException {
        @SuppressWarnings("checkstyle:javadocmethod")
        public UncaughtException(final Throwable cause) {
            super(cause);
        }
    }

    /**
     * Interface used to delegate cancellation operations for a {@link ResultAPI}.
     */
    @AnyThread
    public interface CancellationDelegate {

        /**
         * This method should attempt to cancel the in-progress operation for the result
         * to which this instance was attached. See {@link ResultAPI#cancel()} for more
         * details.
         *
         * @return A {@link ResultAPI} resolving to "true" if cancellation was successful,
         * "false" otherwise.
         */
        default @NonNull
        ResultAPI<Boolean> cancel() {
            return ResultAPI.fromValue(false);
        }
    }

    /**
     * A WPEResult that resolves to AllowOrDeny.ALLOW
     * @deprecated use {@link #allow} instead.
     */
    @Deprecated
    public static final ResultAPI<AllowOrDeny> ALLOW = ResultAPI.fromValue(AllowOrDeny.ALLOW);

    /**
     * A WPEResult that resolves to AllowOrDeny.DENY
     * @deprecated use {@link #deny} instead.
     */
    @Deprecated
    public static final ResultAPI<AllowOrDeny> DENY = ResultAPI.fromValue(AllowOrDeny.DENY);

    /**
     * @return a {@link ResultAPI} that resolves to {@link AllowOrDeny#DENY}
     */
    @AnyThread
    @NonNull
    public static ResultAPI<AllowOrDeny> deny() {
        return ResultAPI.fromValue(AllowOrDeny.DENY);
    }

    /**
     * @return a {@link ResultAPI} that resolves to {@link AllowOrDeny#ALLOW}
     */
    @AnyThread
    @NonNull
    public static ResultAPI<AllowOrDeny> allow() {
        return ResultAPI.fromValue(AllowOrDeny.ALLOW);
    }

    // The default dispatcher for listeners on this WPEResult. Other dispatchers can be specified
    // when the listener is registered.
    private final Dispatcher mDispatcher;
    private boolean mComplete;
    private T mValue;
    private Throwable mError;
    private boolean mIsUncaughtError;
    private SimpleArrayMap<Dispatcher, ArrayList<Runnable>> mListeners = new SimpleArrayMap<>();

    private ResultAPI<?> mParent;
    private CancellationDelegate mCancellationDelegate;

    /**
     * Construct an incomplete WPEResult. Call {@link #complete(Object)} or
     * {@link #completeExceptionally(Throwable)} in order to fulfill the result.
     */
    public ResultAPI() {
        mDispatcher = new HandlerDispatcher(new Handler());
    }

    /**
     * Construct an incomplete WPEResult. Call {@link #complete(Object)} or
     * {@link #completeExceptionally(Throwable)} in order to fulfill the result.
     *
     * @param handler This {@link Handler} will be used for dispatching
     *                listeners registered via {@link #then(OnValueListener, OnExceptionListener)}.
     */
    public ResultAPI(final Handler handler) {
        mDispatcher = new HandlerDispatcher(handler);
    }

    /**
     * This constructs a result that is chained to the specified result.
     *
     * @param from The {@link ResultAPI} to copy.
     */
    public ResultAPI(final ResultAPI<T> from) {
        this();
        completeFrom(from);
    }

    /**
     * Construct a result that is completed with the specified value.
     *
     * @param value The value used to complete the newly created result.
     * @param <U> Type for the result.
     * @return The completed {@link ResultAPI}
     */
    public static @NonNull <U> ResultAPI<U> fromValue(@Nullable final U value) {
        final ResultAPI<U> result = new ResultAPI<>();
        result.complete(value);
        return result;
    }

    /**
     * Construct a result that is completed with the specified {@link Throwable}.
     * May not be null.
     *
     * @param error The exception used to complete the newly created result.
     * @param <T> Type for the result if the result had been completed without exception.
     * @return The completed {@link ResultAPI}
     */
    public static @NonNull <T> ResultAPI<T> fromException(@NonNull final Throwable error) {
        final ResultAPI<T> result = new ResultAPI<>();
        result.completeExceptionally(error);
        return result;
    }

    @Override
    public synchronized int hashCode() {
        int result = 17;
        result = 31 * result + (mComplete ? 1 : 0);
        result = 31 * result + (mValue != null ? mValue.hashCode() : 0);
        result = 31 * result + (mError != null ? mError.hashCode() : 0);
        return result;
    }

    // This can go away once we can rely on java.util.Objects.equals() (API 19)
    private static boolean objectEquals(final Object a, final Object b) {
        return a == b || (a != null && a.equals(b));
    }

    @Override
    public synchronized boolean equals(final Object other) {
        if (other instanceof ResultAPI<?>) {
            final ResultAPI<?> result = (ResultAPI<?>)other;
            return result.mComplete == mComplete &&
                    objectEquals(result.mError, mError) &&
                    objectEquals(result.mValue, mValue);
        }

        return false;
    }

    /**
     * Convenience method for {@link #then(OnValueListener, OnExceptionListener)}.
     *
     * @param valueListener An instance of {@link OnValueListener}, called when the
     *                      {@link ResultAPI} is completed with a value.
     * @param <U> Type of the new result that is returned by the listener.
     * @return A new {@link ResultAPI} that the listener will complete.
     */
    public @NonNull <U> ResultAPI<U> then(@NonNull final OnValueListener<T, U> valueListener) {
        return then(valueListener, null);
    }

    /**
     * Convenience method for {@link #map(OnValueMapper, OnExceptionMapper)}.
     *
     * @param valueMapper An instance of {@link OnValueMapper}, called when
     *                    the {@link ResultAPI} is completed with a value.
     * @param <U> Type of the new value that is returned by the mapper.
     * @return A new {@link ResultAPI} that will contain the mapped value.
     */
    public @NonNull <U> ResultAPI<U> map(@Nullable final OnValueMapper<T, U> valueMapper) {
        return map(valueMapper, null);
    }

    /**
     * Transform the value and error of this {@link ResultAPI}.
     *
     * @param valueMapper An instance of {@link OnValueMapper}, called when
     *                    the {@link ResultAPI} is completed with a value.
     * @param exceptionMapper An instance of {@link OnExceptionMapper}, called
     *                        when the {@link ResultAPI} is completed with an
     *                        exception.
     * @param <U> Type of the new value that is returned by the mapper.
     * @return A new {@link ResultAPI} that will contain the mapped value.
     */
    public @NonNull <U> ResultAPI<U> map(@Nullable final OnValueMapper<T, U> valueMapper,
                                         @Nullable final OnExceptionMapper exceptionMapper) {
        final OnValueListener<T, U> valueListener = valueMapper != null
                ? value -> ResultAPI.fromValue(valueMapper.onValue(value))
                : null;
        final OnExceptionListener<U> exceptionListener = exceptionMapper != null
                ? error -> ResultAPI.fromException(exceptionMapper.onException(error))
                : null;
        return then(valueListener, exceptionListener);
    }

    /**
     * Convenience method for {@link #then(OnValueListener, OnExceptionListener)}.
     *
     * @param exceptionListener An instance of {@link OnExceptionListener}, called when the
     *                          {@link ResultAPI} is completed with an {@link Exception}.
     * @param <U> Type of the new result that is returned by the listener.
     * @return A new {@link ResultAPI} that the listener will complete.
     */
    public @NonNull <U> ResultAPI<U> exceptionally(@NonNull final OnExceptionListener<U> exceptionListener) {
        return then(null, exceptionListener);
    }

    /**
     * Replacement for {@link java.util.function.Consumer} for devices with minApi &lt; 24.
     *
     * @param <T> the type of the input for this consumer.
     */
    // TODO: Remove this when we move to min API 24
    public interface Consumer<T> {
        /**
         * Run this consumer for the given input.
         *
         * @param t the input value.
         */
        @AnyThread
        void accept(@Nullable T t);
    }

    /**
     * Convenience method for {@link #accept(Consumer, Consumer)}.
     *
     * @param valueListener An instance of {@link Consumer}, called when the
     *                      {@link ResultAPI} is completed with a value.
     * @return A new {@link ResultAPI} that the listeners will complete.
     */
    public @NonNull
    ResultAPI<Void> accept(@Nullable final Consumer<T> valueListener) {
        return accept(valueListener, null);
    }

    /**
     * Adds listeners to be called when the {@link ResultAPI} is completed either with
     * a value or {@link Throwable}. Listeners will be invoked on the {@link Looper} returned from
     * {@link #getLooper()}. If null, this method will throw {@link IllegalThreadStateException}.
     *
     * If the result is already complete when this method is called, listeners will be invoked in
     * a future {@link Looper} iteration.
     *
     * @param valueConsumer An instance of {@link Consumer}, called when the
     *                      {@link ResultAPI} is completed with a value.
     * @param exceptionConsumer An instance of {@link Consumer}, called when the
     *                          {@link ResultAPI} is completed with an {@link Throwable}.
     * @return A new {@link ResultAPI} that the listeners will complete.
     */
    public @NonNull
    ResultAPI<Void> accept(@Nullable final Consumer<T> valueConsumer,
                           @Nullable final Consumer<Throwable> exceptionConsumer) {
        final OnValueListener<T, Void> valueListener = valueConsumer == null ? null :
                value -> {
                    valueConsumer.accept(value);
                    return null;
                };

        final OnExceptionListener<Void> exceptionListener = exceptionConsumer == null ? null :
                value -> {
                    exceptionConsumer.accept(value);
                    return null;
                };

        return then(valueListener, exceptionListener);
    }

    /* package */ @NonNull
    ResultAPI<Void> getOrAccept(@Nullable final Consumer<T> valueConsumer) {
        return getOrAccept(valueConsumer, null);
    }

    /* package */ @NonNull
    ResultAPI<Void> getOrAccept(@Nullable final Consumer<T> valueConsumer,
                                @Nullable final Consumer<Throwable> exceptionConsumer) {
        if (haveValue() && valueConsumer != null) {
            valueConsumer.accept(mValue);
            return ResultAPI.fromValue(null);
        }

        if (haveError() && exceptionConsumer != null) {
            exceptionConsumer.accept(mError);
            return ResultAPI.fromValue(null);
        }

        return accept(valueConsumer, exceptionConsumer);
    }

    /**
     * Adds listeners to be called when the {@link ResultAPI} is completed either with
     * a value or {@link Throwable}. Listeners will be invoked on the {@link Looper} returned from
     * {@link #getLooper()}. If null, this method will throw {@link IllegalThreadStateException}.
     *
     * If the result is already complete when this method is called, listeners will be invoked in
     * a future {@link Looper} iteration.
     *
     * @param valueListener An instance of {@link OnValueListener}, called when the
     *                      {@link ResultAPI} is completed with a value.
     * @param exceptionListener An instance of {@link OnExceptionListener}, called when the
     *                          {@link ResultAPI} is completed with an {@link Throwable}.
     * @param <U> Type of the new result that is returned by the listeners.
     * @return A new {@link ResultAPI} that the listeners will complete.
     */
    public @NonNull <U> ResultAPI<U> then(@Nullable final OnValueListener<T, U> valueListener,
                                          @Nullable final OnExceptionListener<U> exceptionListener) {
        if (mDispatcher == null) {
            throw new IllegalThreadStateException("Must have a Handler");
        }

        return thenInternal(mDispatcher, valueListener, exceptionListener);
    }

    private @NonNull <U> ResultAPI<U> thenInternal(@NonNull final Dispatcher dispatcher,
                                                   @Nullable final OnValueListener<T, U> valueListener,
                                                   @Nullable final OnExceptionListener<U> exceptionListener) {
        if (valueListener == null && exceptionListener == null) {
            throw new IllegalArgumentException("At least one listener should be non-null");
        }

        final ResultAPI<U> result = new ResultAPI<U>();
        result.mParent = this;
        thenInternal(dispatcher, () -> {
            try {
                if (haveValue()) {
                    result.completeFrom(valueListener != null ? valueListener.onValue(mValue)
                            : null);
                } else if (!haveError()) {
                    // Listener called without completion?
                    throw new AssertionError();
                } else if (exceptionListener != null) {
                    result.completeFrom(exceptionListener.onException(mError));
                } else {
                    result.mIsUncaughtError = mIsUncaughtError;
                    result.completeExceptionally(mError);
                }
            } catch (final Throwable e) {
                if (!result.mComplete) {
                    result.mIsUncaughtError = true;
                    result.completeExceptionally(e);
                } else if (e instanceof RuntimeException) {
                    // This should only be UncaughtException, but we rethrow all RuntimeExceptions
                    // to avoid squelching logic errors in WPEResult itself.
                    throw (RuntimeException) e;
                }
            }
        });
        return result;
    }

    private synchronized void thenInternal(@NonNull final Dispatcher dispatcher, @NonNull final Runnable listener) {
        if (mComplete) {
            dispatcher.dispatch(listener);
        } else {
            if (!mListeners.containsKey(dispatcher)) {
                mListeners.put(dispatcher, new ArrayList<>(1));
            }
            mListeners.get(dispatcher).add(listener);
        }
    }

    /*private void nativeThen(@NonNull final GeckoCallback accept, @NonNull final GeckoCallback reject) {
        // NB: We could use the lambda syntax here, but given all the layers
        // of abstraction it's helpful to see the types written explicitly.
        thenInternal(DirectDispatcher.sInstance, new OnValueListener<T, Void>() {
            @Override
            public WPEResult<Void> onValue(final T value) {
                accept.call(value);
                return null;
            }
        }, new OnExceptionListener<Void>() {
            @Override
            public WPEResult<Void> onException(final Throwable exception) {
                reject.call(exception);
                return null;
            }
        });
    }*/

    /**
     * @return Get the {@link Looper} that will be used to schedule listeners registered via
     *         {@link #then(OnValueListener, OnExceptionListener)}.
     */
    public @Nullable Looper getLooper() {
        if (mDispatcher == null || !(mDispatcher instanceof HandlerDispatcher)) {
            return null;
        }

        return ((HandlerDispatcher)mDispatcher).mHandler.getLooper();
    }

    /**
     * Returns a new WPEResult that will be completed by this instance. Listeners registered
     * via {@link #then(OnValueListener, OnExceptionListener)} will be run on the specified
     * {@link Handler}.
     *
     * @param handler A {@link Handler} where listeners will be run. May be null.
     * @return A new WPEResult.
     */
    public @NonNull
    ResultAPI<T> withHandler(final @Nullable Handler handler) {
        final ResultAPI<T> result = new ResultAPI<>(handler);
        result.completeFrom(this);
        return result;
    }

    /**
     * Returns a {@link ResultAPI} that is completed when the given {@link ResultAPI}
     * instances are complete.
     *
     * The returned {@link ResultAPI} will resolve with the list of values from the inputs.
     * The list is guaranteed to be in the same order as the inputs.
     *
     * If any of the {@link ResultAPI} fails, the returned result will fail.
     *
     * If no inputs are provided, the returned {@link ResultAPI} will complete with the value
     * <code>null</code>.
     *
     * @param pending the input {@link ResultAPI}s.
     * @param <V> type of the {@link ResultAPI}'s values.
     * @return a {@link ResultAPI} that will complete when all of the inputs are completed or
     *         when at least one of the inputs fail.
     */
    @SuppressWarnings("varargs")
    @SafeVarargs
    @NonNull
    public static <V> ResultAPI<List<V>> allOf(final @NonNull ResultAPI<V>... pending) {
        return allOf(Arrays.asList(pending));
    }

    /**
     * Returns a {@link ResultAPI} that is completed when the given {@link ResultAPI}
     * instances are complete.
     *
     * The returned {@link ResultAPI} will resolve with the list of values from the inputs.
     * The list is guaranteed to be in the same order as the inputs.
     *
     * If any of the {@link ResultAPI} fails, the returned result will fail.
     *
     * If no inputs are provided, the returned {@link ResultAPI} will complete with the value
     * <code>null</code>.
     *
     * @param pending the input {@link ResultAPI}s.
     * @param <V> type of the {@link ResultAPI}'s values.
     * @return a {@link ResultAPI} that will complete when all of the inputs are completed or
     *         when at least one of the inputs fail.
     */
    @NonNull
    public static <V> ResultAPI<List<V>> allOf(
            final @Nullable List<ResultAPI<V>> pending) {
        if (pending == null) {
            return ResultAPI.fromValue(null);
        }

        return new AllOfResult<>(pending);
    }

    private static class AllOfResult<V> extends ResultAPI<List<V>> {
        private boolean mFailed = false;
        private int mResultCount = 0;
        private final List<V> mAccumulator;
        private final List<ResultAPI<V>> mPending;

        public AllOfResult(final @NonNull List<ResultAPI<V>> pending) {
            // Initialize the list with nulls so we can fill it in the same order as the input list
            mAccumulator = new ArrayList<>(Collections.nCopies(pending.size(), null));
            mPending = pending;

            // If the input list is empty, there's nothing to do
            if (pending.size() == 0) {
                complete(mAccumulator);
                return;
            }

            // We use iterators so we can access the index and preserve the list order
            final ListIterator<ResultAPI<V>> it = pending.listIterator();
            while (it.hasNext()) {
                final int index = it.nextIndex();
                it.next().accept(
                        value -> onResult(value, index),
                        this::onError);
            }
        }

        private void onResult(final V value, final int index) {
            if (mFailed) {
                // Some other element in the list already failed, nothing to do here
                return;
            }

            mResultCount++;
            mAccumulator.set(index, value);

            if (mResultCount == mPending.size()) {
                complete(mAccumulator);
            }
        }

        private void onError(final Throwable error) {
            mFailed = true;
            completeExceptionally(error);
        }
    }

    private void dispatchLocked() {
        if (!mComplete) {
            throw new IllegalStateException("Cannot dispatch unless result is complete");
        }

        if (mListeners.isEmpty()) {
            if (mIsUncaughtError) {
                // We have no listeners to forward the uncaught exception to;
                // rethrow the exception to make it visible.
                throw new UncaughtException(mError);
            }
            return;
        }

        if (mDispatcher == null) {
            throw new AssertionError("Shouldn't have listeners with null dispatcher");
        }

        for (int i = 0; i < mListeners.size(); ++i) {
            final Dispatcher dispatcher = mListeners.keyAt(i);
            final ArrayList<Runnable> jobs = mListeners.valueAt(i);
            dispatcher.dispatch(() -> {
                for (final Runnable job : jobs) {
                    job.run();
                }
            });
        }
        mListeners.clear();
    }

    /**
     * Completes this result based on another result.
     *
     * @param other The result that this result should mirror
     */
    private void completeFrom(final ResultAPI<T> other) {
        if (other == null) {
            complete(null);
            return;
        }

        this.mCancellationDelegate = other.mCancellationDelegate;
        other.thenInternal(DirectDispatcher.sInstance, () -> {
            if (other.haveValue()) {
                complete(other.mValue);
            } else {
                mIsUncaughtError = other.mIsUncaughtError;
                completeExceptionally(other.mError);
            }
        });
    }

    /**
     * Return the value of this result, waiting for it to be completed
     * if necessary. If the result is completed with an exception
     * it will be rethrown here.
     * <p>
     * You must not call this method if the current thread has a {@link Looper} due to
     * the possibility of a deadlock. If this occurs, {@link IllegalStateException}
     * is thrown.
     *
     * @return The value of this result.
     * @throws Throwable The {@link Throwable} contained in this result, if any.
     * @throws IllegalThreadStateException if this method is called on a thread that has a {@link Looper}.
     */
    public synchronized @Nullable T poll() throws Throwable {
        if (Looper.myLooper() != null) {
            throw new IllegalThreadStateException("Cannot poll indefinitely from thread with Looper");
        }

        return poll(Long.MAX_VALUE);
    }

    /**
     * Return the value of this result, waiting for it to be completed
     * if necessary. If the result is completed with an exception
     * it will be rethrown here.
     *
     * Caution is advised if the caller is on a thread with a {@link Looper}, as it's possible to
     * effectively deadlock in cases when the work is being completed on the calling thread. It's
     * preferable to use {@link #then(OnValueListener, OnExceptionListener)} in such circumstances,
     * but if you must use this method consider a small timeout value.
     *
     * @param timeoutMillis Number of milliseconds to wait for the result
     *                      to complete.
     * @return The value of this result.
     * @throws Throwable The {@link Throwable} contained in this result, if any.
     * @throws TimeoutException if we wait more than timeoutMillis before the result
     *                          is completed.
     */
    public synchronized @Nullable T poll(final long timeoutMillis) throws Throwable {
        final long start = SystemClock.uptimeMillis();
        long remaining = timeoutMillis;
        while (!mComplete && remaining > 0) {
            try {
                wait(remaining);
            } catch (final InterruptedException e) {
            }

            remaining = timeoutMillis - (SystemClock.uptimeMillis() - start);
        }

        if (!mComplete) {
            throw new TimeoutException();
        }

        if (haveError()) {
            throw mError;
        }

        return mValue;
    }

    /**
     * Complete the result with the specified value. IllegalStateException is thrown
     * if the result is already complete.
     *
     * @param value The value used to complete the result.
     * @throws IllegalStateException If the result is already completed.
     */
    public synchronized void complete(final @Nullable T value) {
        if (mComplete) {
            throw new IllegalStateException("result is already complete");
        }

        mValue = value;
        mComplete = true;

        dispatchLocked();
        notifyAll();
    }

    /**
     * Complete the result with the specified {@link Throwable}. IllegalStateException is thrown
     * if the result is already complete.
     *
     * @param exception The {@link Throwable} used to complete the result.
     * @throws IllegalStateException If the result is already completed.
     */
    public synchronized void completeExceptionally(@NonNull final Throwable exception) {
        if (mComplete) {
            throw new IllegalStateException("result is already complete");
        }

        if (exception == null) {
            throw new IllegalArgumentException("Throwable must not be null");
        }

        mError = exception;
        mComplete = true;

        dispatchLocked();
        notifyAll();
    }

    /**
     * An interface used to deliver values to listeners of a {@link ResultAPI}
     * @param <T> Type of the value delivered via {@link #onValue(Object)}
     * @param <U> Type of the value for the result returned from {@link #onValue(Object)}
     */
    public interface OnValueListener<T, U> {
        /**
         * Called when a {@link ResultAPI} is completed with a value. Will be
         * called on the same thread where the WPEResult was created or on
         * the {@link Handler} provided via {@link #withHandler(Handler)}.
         *
         * @param value The value of the {@link ResultAPI}
         * @return Result used to complete the next result in the chain. May be null.
         * @throws Throwable Exception used to complete next result in the chain.
         */
        @AnyThread
        @Nullable
        ResultAPI<U> onValue(@Nullable T value) throws Throwable;
    }

    /**
     * An interface used to map {@link ResultAPI} values.
     *
     * @param <T> Type of the value delivered via {@link #onValue}
     * @param <U> Type of the new value returned by {@link #onValue}
     */
    public interface OnValueMapper<T, U> {
        /**
         * Called when a {@link ResultAPI} is completed with a value.
         * Will be called on the same thread where the WPEResult was created
         * or on the {@link Handler} provided via {@link #withHandler(Handler)}.
         *
         * @param value The value of the {@link ResultAPI}
         * @return Value used to complete the next result in the chain. May be null.
         * @throws Throwable Exception used to complete next result in the chain.
         */
        @AnyThread
        @Nullable U onValue(@Nullable T value) throws Throwable;
    }

    /**
     * An interface used to map {@link ResultAPI} exceptions.
     */
    public interface OnExceptionMapper {
        /**
         * Called when a {@link ResultAPI} is completed with an exception.
         * Will be called on the same thread where the WPEResult was created
         * or on the {@link Handler} provided via {@link #withHandler(Handler)}.
         *
         * @param exception Exception that completed the result.
         * @return Exception used to complete the next result in the chain. May be null.
         * @throws Throwable Exception used to complete next result in the chain.
         */
        @AnyThread
        @Nullable Throwable onException(@NonNull Throwable exception) throws Throwable;
    }

    /**
     * An interface used to deliver exceptions to listeners of a {@link ResultAPI}
     *
     * @param <V> Type of the vale for the result returned from {@link #onException(Throwable)}
     */
    public interface OnExceptionListener<V> {
        /**
         * Called when a {@link ResultAPI} is completed with an exception.
         * Will be called on the same thread where the WPEResult was created
         * or on the {@link Handler} provided via {@link #withHandler(Handler)}.
         *
         * @param exception Exception that completed the result.
         * @return Result used to complete the next result in the chain. May be null.
         * @throws Throwable Exception used to complete next result in the chain.
         */
        @AnyThread
        @Nullable
        ResultAPI<V> onException(@NonNull Throwable exception) throws Throwable;
    }

    private boolean haveValue() {
        return mComplete && mError == null;
    }

    private boolean haveError() {
        return mComplete && mError != null;
    }

    /**
     * Attempts to cancel the operation associated with this result.
     *
     * If this result has a {@link CancellationDelegate} attached via
     * {@link #setCancellationDelegate(CancellationDelegate)}, the return value
     * will be the result of calling {@link CancellationDelegate#cancel()} on that instance.
     * Otherwise, if this result is chained to another result
     * (via return value from {@link OnValueListener}), we will walk up the chain until
     * a CancellationDelegate is found and run it. If no CancellationDelegate is found,
     * a result resolving to "false" will be returned.
     *
     * If this result is already complete, the returned result will always resolve to false.
     *
     * If the returned result resolves to true, this result will be completed
     * with a {@link CancellationException}.
     *
     * @return A WPEResult resolving to a boolean indicating success or failure of the cancellation attempt.
     */
    public synchronized @NonNull
    ResultAPI<Boolean> cancel() {
        if (haveValue() || haveError()) {
            return ResultAPI.fromValue(false);
        }

        if (mCancellationDelegate != null) {
            return mCancellationDelegate.cancel().then(value -> {
                if (value) {
                    try {
                        this.completeExceptionally(new CancellationException());
                    } catch (final IllegalStateException e) {
                        // Can't really do anything about this.
                    }
                }
                return ResultAPI.fromValue(value);
            });
        }

        if (mParent != null) {
            return mParent.cancel();
        }

        return ResultAPI.fromValue(false);
    }

    /**
     * Sets the instance of {@link CancellationDelegate} that will be invoked by
     * {@link #cancel()}.
     *
     * @param delegate an instance of CancellationDelegate.
     */
    public void setCancellationDelegate(final @Nullable CancellationDelegate delegate) {
        mCancellationDelegate = delegate;
    }

}
