package org.mozilla.vrbrowser.browser.api;

import android.view.inputmethod.CursorAnchorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.UiThread;

import org.mozilla.vrbrowser.browser.engine.Session;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Interface that SessionTextInput uses for performing operations such as opening and closing
 * the software keyboard. If the delegate is not set, these operations are forwarded to the
 * system {@link android.view.inputmethod.InputMethodManager} automatically.
 */
public interface TextInputDelegate {
    /** Restarting input due to an input field gaining focus. */
    int RESTART_REASON_FOCUS = 0;
    /** Restarting input due to an input field losing focus. */
    int RESTART_REASON_BLUR = 1;
    /**
     * Restarting input due to the content of the input field changing. For example, the
     * input field type may have changed, or the current composition may have been committed
     * outside of the input method.
     */
    int RESTART_REASON_CONTENT_CHANGE = 2;

    /**
     * Reset the input method, and discard any existing states such as the current composition
     * or current autocompletion. Because the current focused editor may have changed, as
     * part of the reset, a custom input method would normally call {@link
     * SessionTextInput#onCreateInputConnection} to update its knowledge of the focused editor.
     * Note that {@code restartInput} should be used to detect changes in focus, rather than
     * {@link #showSoftInput} or {@link #hideSoftInput}, because focus changes are not always
     * accompanied by requests to show or hide the soft input. This method is always called,
     * even in viewless mode.
     *
     * @param session Session instance.
     * @param reason Reason for the reset.
     */
    @UiThread
    default void restartInput(@NonNull final SessionAPI session, @RestartReason final int reason) {}

    /**
     * Display the soft input. May be called consecutively, even if the soft input is
     * already shown. This method is always called, even in viewless mode.
     *
     * @param session Session instance.
     * @see #hideSoftInput
     * */
    @UiThread
    default void showSoftInput(@NonNull final SessionAPI session) {}

    /**
     * Hide the soft input. May be called consecutively, even if the soft input is
     * already hidden. This method is always called, even in viewless mode.
     *
     * @param session Session instance.
     * @see #showSoftInput
     * */
    @UiThread
    default void hideSoftInput(@NonNull final SessionAPI session) {}

    /**
     * Update the soft input on the current selection. This method is <i>not</i> called
     * in viewless mode.
     *
     * @param session Session instance.
     * @param selStart Start offset of the selection.
     * @param selEnd End offset of the selection.
     * @param compositionStart Composition start offset, or -1 if there is no composition.
     * @param compositionEnd Composition end offset, or -1 if there is no composition.
     */
    @UiThread
    default void updateSelection(@NonNull final SessionAPI session, final int selStart, final int selEnd,
                                 final int compositionStart, final int compositionEnd) {}

    /**
     * Update the soft input on the current extracted text, as requested through
     * {@link android.view.inputmethod.InputConnection#getExtractedText}.
     * Consequently, this method is <i>not</i> called in viewless mode.
     *
     * @param session Session instance.
     * @param request The extract text request.
     * @param text The extracted text.
     */
    @UiThread
    default void updateExtractedText(@NonNull final SessionAPI session,
                                     @NonNull final ExtractedTextRequest request,
                                     @NonNull final ExtractedText text) {}

    /**
     * Update the cursor-anchor information as requested through
     * {@link android.view.inputmethod.InputConnection#requestCursorUpdates}.
     * Consequently, this method is <i>not</i> called in viewless mode.
     *
     * @param session Session instance.
     * @param info Cursor-anchor information.
     */
    @UiThread
    default void updateCursorAnchorInfo(@NonNull final SessionAPI session,
                                        @NonNull final CursorAnchorInfo info) {}
}

@Retention(RetentionPolicy.SOURCE)
@IntDef({TextInputDelegate.RESTART_REASON_FOCUS, TextInputDelegate.RESTART_REASON_BLUR,
        TextInputDelegate.RESTART_REASON_CONTENT_CHANGE})
        /* package */ @interface RestartReason {}