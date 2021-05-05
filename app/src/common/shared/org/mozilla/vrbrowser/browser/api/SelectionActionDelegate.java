package org.mozilla.vrbrowser.browser.api;

import android.graphics.RectF;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringDef;

import org.mozilla.vrbrowser.browser.engine.Session;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collection;
import java.util.HashSet;

public interface SelectionActionDelegate {
    /**
     * The selection is collapsed at a single position.
     */
    final int FLAG_IS_COLLAPSED = 1;
    /**
     * The selection is inside editable content such as an input element or
     * contentEditable node.
     */
    final int FLAG_IS_EDITABLE = 2;
    /**
     * The selection is inside a password field.
     */
    final int FLAG_IS_PASSWORD = 4;

    /**
     * Hide selection actions and cause {@link #onHideAction} to be called.
     */
    final String ACTION_HIDE = "org.mozilla.geckoview.HIDE";
    /**
     * Copy onto the clipboard then delete the selected content. Selection
     * must be editable.
     */
    final String ACTION_CUT = "org.mozilla.geckoview.CUT";
    /**
     * Copy the selected content onto the clipboard.
     */
    final String ACTION_COPY = "org.mozilla.geckoview.COPY";
    /**
     * Delete the selected content. Selection must be editable.
     */
    final String ACTION_DELETE = "org.mozilla.geckoview.DELETE";
    /**
     * Replace the selected content with the clipboard content. Selection
     * must be editable.
     */
    final String ACTION_PASTE = "org.mozilla.geckoview.PASTE";
    /**
     * Select the entire content of the document or editor.
     */
    final String ACTION_SELECT_ALL = "org.mozilla.geckoview.SELECT_ALL";
    /**
     * Clear the current selection. Selection must not be editable.
     */
    final String ACTION_UNSELECT = "org.mozilla.geckoview.UNSELECT";
    /**
     * Collapse the current selection to its start position.
     * Selection must be editable.
     */
    final String ACTION_COLLAPSE_TO_START = "org.mozilla.geckoview.COLLAPSE_TO_START";
    /**
     * Collapse the current selection to its end position.
     * Selection must be editable.
     */
    final String ACTION_COLLAPSE_TO_END = "org.mozilla.geckoview.COLLAPSE_TO_END";

    /**
     * Represents attributes of a selection.
     */
    class Selection {
        /**
         * Flags describing the current selection, as a bitwise combination
         * of the {@link #FLAG_IS_COLLAPSED FLAG_*} constants.
         */
        public final @SelectionActionDelegateFlag int flags;

        /**
         * Text content of the current selection. An empty string indicates the selection
         * is collapsed or the selection cannot be represented as plain text.
         */
        public final @NonNull
        String text;

        /**
         * The bounds of the current selection in client coordinates. Use getClientToScreenMatrix to perform transformation to screen
         * coordinates.
         */
        public final @Nullable RectF clientRect;

        /**
         * Set of valid actions available through {@link Selection#execute(String)}
         */
        public final @NonNull @SelectionActionDelegateAction
        Collection<String> availableActions;

        /**
         * Empty constructor for tests.
         */
        protected Selection() {
            flags = 0;
            text = "";
            clientRect = null;
            availableActions = new HashSet<>();
        }

        /**
         * Checks if the passed action is available
         * @param action An {@link SelectionActionDelegate} to perform
         * @return True if the action is available.
         */
        public boolean isActionAvailable(@NonNull @SelectionActionDelegateAction final String action) {
            return availableActions.contains(action);
        }

        /**
         * Execute an {@link SelectionActionDelegate} action.
         *
         * @throws IllegalStateException If the action was not available.
         * @param action A {@link SelectionActionDelegate} action.
         */
        public void execute(@NonNull @SelectionActionDelegateAction final String action) {
        }

        /**
         * Hide selection actions and cause {@link #onHideAction} to be called.
         *
         * @throws IllegalStateException If the action was not available.
         */
        public void hide() {
            execute(ACTION_HIDE);
        }

        /**
         * Copy onto the clipboard then delete the selected content.
         *
         * @throws IllegalStateException If the action was not available.
         */
        public void cut() {
            execute(ACTION_CUT);
        }

        /**
         * Copy the selected content onto the clipboard.
         *
         * @throws IllegalStateException If the action was not available.
         */
        public void copy() {
            execute(ACTION_COPY);
        }

        /**
         * Delete the selected content.
         *
         * @throws IllegalStateException If the action was not available.
         */
        public void delete() {
            execute(ACTION_DELETE);
        }

        /**
         * Replace the selected content with the clipboard content.
         *
         * @throws IllegalStateException If the action was not available.
         */
        public void paste() {
            execute(ACTION_PASTE);
        }

        /**
         * Select the entire content of the document or editor.
         *
         * @throws IllegalStateException If the action was not available.
         */
        public void selectAll() {
            execute(ACTION_SELECT_ALL);
        }

        /**
         * Clear the current selection.
         *
         * @throws IllegalStateException If the action was not available.
         */
        public void unselect() {
            execute(ACTION_UNSELECT);
        }

        /**
         * Collapse the current selection to its start position.
         *
         * @throws IllegalStateException If the action was not available.
         */
        public void collapseToStart() {
            execute(ACTION_COLLAPSE_TO_START);
        }

        /**
         * Collapse the current selection to its end position.
         *
         * @throws IllegalStateException If the action was not available.
         */
        public void collapseToEnd() {
            execute(ACTION_COLLAPSE_TO_END);
        }
    }

    /**
     * Selection actions are available. Selection actions become available when the
     * user selects some content in the document or editor. Inside an editor,
     * selection actions can also become available when the user explicitly requests
     * editor action UI, for example by tapping on the caret handle.
     *
     * In response to this callback, applications typically display a toolbar
     * containing the selection actions. To perform a certain action, check if the action
     * is available with {@link Selection#isActionAvailable} then either use the relevant
     * helper method or {@link Selection#execute}
     *
     * Once an {@link #onHideAction} call (with particular reasons) or another {@link
     * #onShowActionRequest} call is received, the previous Selection object is no longer
     * usable.
     *
     * @param session The GeckoSession that initiated the callback.
     * @param selection Current selection attributes and Callback object for performing built-in
     *                  actions. May be used multiple times to perform multiple actions at once.
     */
    default void onShowActionRequest(@NonNull final SessionAPI session,
                                     @NonNull final Selection selection) {}

    /**
     * Actions are no longer available due to the user clearing the selection.
     */
    final int HIDE_REASON_NO_SELECTION = 0;
    /**
     * Actions are no longer available due to the user moving the selection out of view.
     * Previous actions are still available after a callback with this reason.
     */
    final int HIDE_REASON_INVISIBLE_SELECTION = 1;
    /**
     * Actions are no longer available due to the user actively changing the
     * selection. {@link #onShowActionRequest} may be called again once the user has
     * set a selection, if the new selection has available actions.
     */
    final int HIDE_REASON_ACTIVE_SELECTION = 2;
    /**
     * Actions are no longer available due to the user actively scrolling the page.
     * {@link #onShowActionRequest} may be called again once the user has stopped
     * scrolling the page, if the selection is still visible. Until then, previous
     * actions are still available after a callback with this reason.
     */
    final int HIDE_REASON_ACTIVE_SCROLL = 3;

    /**
     * Previous actions are no longer available due to the user interacting with the
     * page. Applications typically hide the action toolbar in response.
     *
     * @param session The GeckoSession that initiated the callback.
     * @param reason The reason that actions are no longer available, as one of the
     * {@link #HIDE_REASON_NO_SELECTION HIDE_REASON_*} constants.
     */
    default void onHideAction(@NonNull final SessionAPI session,
                              @SelectionActionDelegateHideReason final int reason) {}

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            SelectionActionDelegate.ACTION_HIDE,
            SelectionActionDelegate.ACTION_CUT,
            SelectionActionDelegate.ACTION_COPY,
            SelectionActionDelegate.ACTION_DELETE,
            SelectionActionDelegate.ACTION_PASTE,
            SelectionActionDelegate.ACTION_SELECT_ALL,
            SelectionActionDelegate.ACTION_UNSELECT,
            SelectionActionDelegate.ACTION_COLLAPSE_TO_START,
            SelectionActionDelegate.ACTION_COLLAPSE_TO_END})
            /* package */ @interface SelectionActionDelegateAction {}

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(flag = true, value = {
            SelectionActionDelegate.FLAG_IS_COLLAPSED,
            SelectionActionDelegate.FLAG_IS_EDITABLE})
            /* package */ @interface SelectionActionDelegateFlag {}

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            SelectionActionDelegate.HIDE_REASON_NO_SELECTION,
            SelectionActionDelegate.HIDE_REASON_INVISIBLE_SELECTION,
            SelectionActionDelegate.HIDE_REASON_ACTIVE_SELECTION,
            SelectionActionDelegate.HIDE_REASON_ACTIVE_SCROLL})
            /* package */ @interface SelectionActionDelegateHideReason {}
}

