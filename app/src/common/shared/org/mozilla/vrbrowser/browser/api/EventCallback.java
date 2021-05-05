package org.mozilla.vrbrowser.browser.api;


import androidx.annotation.Nullable;

/**
 * Callback interface for Gecko requests.
 *
 * For each instance of EventCallback, exactly one of sendResponse, sendError, or sendCancel
 * must be called to prevent observer leaks. If more than one send* method is called, or if a
 * single send method is called multiple times, an {@link IllegalStateException} will be thrown.
 */
public interface EventCallback {
    /**
     * Sends a success response with the given data.
     *
     * @param response The response data to send to Gecko. Can be any of the types accepted by
     *                 JSONObject#put(String, Object).
     */
    void sendSuccess(Object response);

    /**
     * Sends an error response with the given data.
     *
     * @param response The response data to send to Gecko. Can be any of the types accepted by
     *                 JSONObject#put(String, Object).
     */
    void sendError(Object response);

    /**
     * Resolve this Event callback with the result from the {@link ResultAPI}.
     *
     * @param response the result that will be used for this callback.
     */
    default <T> void resolveTo(final @Nullable ResultAPI<T> response) {
        if (response == null) {
            sendSuccess(null);
            return;
        }
        response.accept(this::sendSuccess, throwable -> {
            // Don't propagate Errors, just crash
            if (!(throwable instanceof Exception)) {
                throw new ResultAPI.UncaughtException(throwable);
            }
            sendError(throwable.getMessage());
        });
    }
}