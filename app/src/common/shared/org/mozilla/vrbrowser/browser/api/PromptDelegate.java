package org.mozilla.vrbrowser.browser.api;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import org.mozilla.vrbrowser.browser.engine.Session;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Session applications implement this interface to handle prompts triggered by
 * content in the Session, such as alerts, authentication dialogs, and select list
 * pickers.
 **/
public interface PromptDelegate {
    /**
     * PromptResponse is an opaque class created upon confirming or dismissing a
     * prompt.
     */
    public class PromptResponse {
        private final BasePrompt mPrompt;

        /* package */ PromptResponse(@NonNull final BasePrompt prompt) {
            mPrompt = prompt;
        }

        /* package */ void dispatch(@NonNull final EventCallback callback) {
            if (mPrompt == null) {
                throw new RuntimeException("Trying to confirm/dismiss a null prompt.");
            }
            mPrompt.dispatch(callback);
        }
    }

    // Prompt classes.
    public class BasePrompt {
        private boolean mIsCompleted;
        private boolean mIsConfirmed;
        private Bundle mResult;

        /**
         * The title of this prompt; may be null.
         */
        public final @Nullable
        String title;

        private BasePrompt(@Nullable final String title) {
            this.title = title;
            mIsConfirmed = false;
            mIsCompleted = false;
        }

        /* package */ Bundle ensureResult() {
            if (mResult == null) {
                // Usually result object contains two items.
                mResult = new Bundle(2);
            }
            return mResult;
        }

        @UiThread
        protected @NonNull PromptResponse confirm() {
            if (mIsCompleted) {
                throw new RuntimeException("Cannot confirm/dismiss a Prompt twice.");
            }

            mIsCompleted = true;
            mIsConfirmed = true;
            return new PromptResponse(this);
        }

        /**
         * This dismisses the prompt without sending any meaningful information back
         * to content.
         *
         * @return A {@link PromptResponse} with which you can complete the
         *         WPEResult that corresponds to this prompt.
         */
        @UiThread
        public @NonNull PromptResponse dismiss() {
            if (mIsCompleted) {
                throw new RuntimeException("Cannot confirm/dismiss a Prompt twice.");
            }

            mIsCompleted = true;
            return new PromptResponse(this);
        }

        /**
         * This returns true if the prompt has already been confirmed or dismissed.
         *
         * @return A boolean which is true if the prompt has been confirmed or dismissed,
         *         and false otherwise.
         */
        @UiThread
        public boolean isComplete() {
            return mIsCompleted;
        }

        /* package */ void dispatch(@NonNull final EventCallback callback) {
            if (!mIsCompleted) {
                throw new RuntimeException("Trying to dispatch an incomplete prompt.");
            }

            if (!mIsConfirmed) {
                callback.sendSuccess(null);
            } else {
                callback.sendSuccess(mResult);
            }
        }
    }

    /**
     * BeforeUnloadPrompt represents the onbeforeunload prompt.
     * See https://developer.mozilla.org/en-US/docs/Web/API/WindowEventHandlers/onbeforeunload
     */
    class BeforeUnloadPrompt extends BasePrompt {
        protected BeforeUnloadPrompt() {
            super(null);
        }

        /**
         * Confirms the prompt.
         *
         * @param allowOrDeny whether the navigation should be allowed to continue or not.
         *
         * @return A {@link PromptResponse} which can be used to complete
         *         the WPEResult associated with this prompt.
         */
        @UiThread
        public @NonNull PromptResponse confirm(final @Nullable AllowOrDeny allowOrDeny) {
            ensureResult().putBoolean("allow", allowOrDeny != AllowOrDeny.DENY);
            return super.confirm();
        }
    }

    /**
     * RepostConfirmPrompt represents a prompt shown whenever the browser
     * needs to resubmit POST data (e.g. due to page refresh).
     */
    class RepostConfirmPrompt extends BasePrompt {
        protected RepostConfirmPrompt() {
            super(null);
        }

        /**
         * Confirms the prompt.
         *
         * @param allowOrDeny whether the browser should allow resubmitting
         *                    data.
         *
         * @return A {@link PromptResponse} which can be used to complete
         *         the WPEResult associated with this prompt.
         */
        @UiThread
        public @NonNull PromptResponse confirm(final @Nullable AllowOrDeny allowOrDeny) {
            ensureResult().putBoolean("allow", allowOrDeny != AllowOrDeny.DENY);
            return super.confirm();
        }
    }

    /**
     * AlertPrompt contains the information necessary to represent a JavaScript
     * alert() call from content; it can only be dismissed, not confirmed.
     */
    public class AlertPrompt extends BasePrompt {
        /**
         * The message to be displayed with this alert; may be null.
         */
        public final @Nullable String message;

        protected AlertPrompt(@Nullable final String title,
                              @Nullable final String message) {
            super(title);
            this.message = message;
        }
    }

    /**
     * ButtonPrompt contains the information necessary to represent a JavaScript
     * confirm() call from content.
     */
    public class ButtonPrompt extends BasePrompt {
        @Retention(RetentionPolicy.SOURCE)
        @IntDef({Type.POSITIVE, Type.NEGATIVE})
                /* package */ @interface ButtonType {}

        public static class Type {
            /**
             * Index of positive response button (eg, "Yes", "OK")
             */
            public static final int POSITIVE = 0;

            /**
             * Index of negative response button (eg, "No", "Cancel")
             */
            public static final int NEGATIVE = 2;

            protected Type() {}
        }

        /**
         * The message to be displayed with this prompt; may be null.
         */
        public final @Nullable String message;

        protected ButtonPrompt(@Nullable final String title,
                               @Nullable final String message) {
            super(title);
            this.message = message;
        }

        /**
         * Confirms this prompt, returning the selected button to content.
         *
         * @param selection An int representing the selected button, must be
         *                  one of {@link Type}.
         *
         * @return A {@link PromptResponse} which can be used to complete
         *         the WPEResult associated with this prompt.
         */
        @UiThread
        public @NonNull PromptResponse confirm(@ButtonType final int selection) {
            ensureResult().putInt("button", selection);
            return super.confirm();
        }
    }

    /**
     * TextPrompt contains the information necessary to represent a Javascript
     * prompt() call from content.
     */
    public class TextPrompt extends BasePrompt {
        /**
         * The message to be displayed with this prompt; may be null.
         */
        public final @Nullable String message;

        /**
         * The default value for the text field; may be null.
         */
        public final @Nullable String defaultValue;

        protected TextPrompt(@Nullable final String title,
                             @Nullable final String message,
                             @Nullable final String defaultValue) {
            super(title);
            this.message = message;
            this.defaultValue = defaultValue;
        }

        /**
         * Confirms this prompt, returning the input text to content.
         *
         * @param text A String containing the text input given by the user.
         *
         * @return A {@link PromptResponse} which can be used to complete
         *         the WPEResult associated with this prompt.
         */
        @UiThread
        public @NonNull PromptResponse confirm(@NonNull final String text) {
            ensureResult().putString("text", text);
            return super.confirm();
        }
    }

    /**
     * AuthPrompt contains the information necessary to represent an HTML
     * authorization prompt generated by content.
     */
    public class AuthPrompt extends BasePrompt {
        public static class AuthOptions {
            @Retention(RetentionPolicy.SOURCE)
            @IntDef(flag = true,
                    value = {Flags.HOST, Flags.PROXY, Flags.ONLY_PASSWORD,
                            Flags.PREVIOUS_FAILED, Flags.CROSS_ORIGIN_SUB_RESOURCE})
                    /* package */ @interface AuthFlag {}

            /**
             * Auth prompt flags.
             */
            public static class Flags {
                /**
                 * The auth prompt is for a network host.
                 */
                public static final int HOST = 1;
                /**
                 * The auth prompt is for a proxy.
                 */
                public static final int PROXY = 2;
                /**
                 * The auth prompt should only request a password.
                 */
                public static final int ONLY_PASSWORD = 8;
                /**
                 * The auth prompt is the result of a previous failed login.
                 */
                public static final int PREVIOUS_FAILED = 16;
                /**
                 * The auth prompt is for a cross-origin sub-resource.
                 */
                public static final int CROSS_ORIGIN_SUB_RESOURCE = 32;

                protected Flags() {}
            }

            @Retention(RetentionPolicy.SOURCE)
            @IntDef({Level.NONE, Level.PW_ENCRYPTED, Level.SECURE})
                    /* package */ @interface AuthLevel {}

            /**
             * Auth prompt levels.
             */
            public static class Level {
                /**
                 * The auth request is unencrypted or the encryption status is unknown.
                 */
                public static final int NONE = 0;
                /**
                 * The auth request only encrypts password but not data.
                 */
                public static final int PW_ENCRYPTED = 1;
                /**
                 * The auth request encrypts both password and data.
                 */
                public static final int SECURE = 2;

                protected Level() {}
            }

            /**
             * An int bit-field of {@link Flags}.
             */
            public @AuthFlag final int flags;

            /**
             * A string containing the URI for the auth request or null if unknown.
             */
            public @Nullable final String uri;

            /**
             * An int, one of {@link Level}, indicating level of encryption.
             */
            public @AuthLevel final int level;

            /**
             * A string containing the initial username or null if password-only.
             */
            public @Nullable final String username;

            /**
             * A string containing the initial password.
             */
            public @Nullable final String password;

            /* package */ AuthOptions(final Bundle options) {
                flags = options.getInt("flags");
                uri = options.getString("uri");
                level = options.getInt("level");
                username = options.getString("username");
                password = options.getString("password");
            }

            /**
             * Empty constructor for tests
             */
            protected AuthOptions() {
                flags = 0;
                uri = "";
                level = 0;
                username = "";
                password = "";
            }
        }

        /**
         * The message to be displayed with this prompt; may be null.
         */
        public final @Nullable String message;

        /**
         * The {@link AuthOptions} that describe the type of authorization prompt.
         */
        public final @NonNull AuthOptions authOptions;

        protected AuthPrompt(@Nullable final String title,
                             @Nullable final String message,
                             @NonNull final AuthOptions authOptions) {
            super(title);
            this.message = message;
            this.authOptions = authOptions;
        }

        /**
         * Confirms this prompt with just a password, returning the password to content.
         *
         * @param password A String containing the password input by the user.
         *
         * @return A {@link PromptResponse} which can be used to complete
         *         the WPEResult associated with this prompt.
         */
        @UiThread
        public @NonNull PromptResponse confirm(@NonNull final String password) {
            ensureResult().putString("password", password);
            return super.confirm();
        }

        /**
         * Confirms this prompt with a username and password, returning both to content.
         *
         * @param username A String containing the username input by the user.
         * @param password A String containing the password input by the user.
         *
         * @return A {@link PromptResponse} which can be used to complete
         *         the WPEResult associated with this prompt.
         */
        @UiThread
        public @NonNull PromptResponse confirm(@NonNull final String username,
                                               @NonNull final String password) {
            ensureResult().putString("username", username);
            ensureResult().putString("password", password);
            return super.confirm();
        }
    }

    /**
     * ChoicePrompt contains the information necessary to display a menu or list prompt
     * generated by content.
     */
    public class ChoicePrompt extends BasePrompt {
        public static class Choice {
            /**
             * A boolean indicating if the item is disabled. Item should not be
             * selectable if this is true.
             */
            public final boolean disabled;

            /**
             * A String giving the URI of the item icon, or null if none exists
             * (only valid for menus)
             */
            public final @Nullable String icon;

            /**
             * A String giving the ID of the item or group
             */
            public final @NonNull String id;

            /**
             * A Choice array of sub-items in a group, or null if not a group
             */
            public final @Nullable Choice[] items;

            /**
             * A string giving the label for displaying the item or group
             */
            public final @NonNull String label;

            /**
             * A boolean indicating if the item should be pre-selected
             * (pre-checked for menu items)
             */
            public final boolean selected;

            /**
             * A boolean indicating if the item should be a menu separator
             * (only valid for menus)
             */
            public final boolean separator;

            /* package */ Choice(final Bundle choice) {
                disabled = choice.getBoolean("disabled");
                icon = choice.getString("icon");
                id = choice.getString("id");
                label = choice.getString("label");
                selected = choice.getBoolean("selected");
                separator = choice.getBoolean("separator");
                final Bundle[] choices = (Bundle[])choice.get("items") ;// = choice.getBundleArray("items");
                if (choices == null) {
                    items = null;
                } else {
                    items = new Choice[choices.length];
                    for (int i = 0; i < choices.length; i++) {
                        items[i] = new Choice(choices[i]);
                    }
                }
            }

            /**
             * Empty constructor for tests.
             */
            protected Choice() {
                disabled = false;
                icon = "";
                id = "";
                label = "";
                selected = false;
                separator = false;
                items = null;
            }
        }

        @Retention(RetentionPolicy.SOURCE)
        @IntDef({Type.MENU, Type.SINGLE, Type.MULTIPLE})
                /* package */ @interface ChoiceType {}

        public static class Type {
            /**
             * Display choices in a menu that dismisses as soon as an item is chosen.
             */
            public static final int MENU = 1;

            /**
             * Display choices in a list that allows a single selection.
             */
            public static final int SINGLE = 2;

            /**
             * Display choices in a list that allows multiple selections.
             */
            public static final int MULTIPLE = 3;

            protected Type() {}
        }

        /**
         * The message to be displayed with this prompt; may be null.
         */
        public final @Nullable String message;

        /**
         * One of {@link Type}.
         */
        public final @ChoiceType int type;

        /**
         * An array of {@link Choice} representing possible choices.
         */
        public final @NonNull Choice[] choices;

        protected ChoicePrompt(@Nullable final String title,
                               @Nullable final String message,
                               @ChoiceType final int type,
                               @NonNull final Choice[] choices) {
            super(title);
            this.message = message;
            this.type = type;
            this.choices = choices;
        }

        /**
         * Confirms this prompt with the string id of a single choice.
         *
         * @param selectedId The string ID of the selected choice.
         *
         * @return A {@link PromptResponse} which can be used to complete
         *         the WPEResult associated with this prompt.
         */
        @UiThread
        public @NonNull PromptResponse confirm(@NonNull final String selectedId) {
            return confirm(new String[] { selectedId });
        }

        /**
         * Confirms this prompt with the string ids of multiple choices
         *
         * @param selectedIds The string IDs of the selected choices.
         *
         * @return A {@link PromptResponse} which can be used to complete
         *         the WPEResult associated with this prompt.
         */
        @UiThread
        public @NonNull PromptResponse confirm(@NonNull final String[] selectedIds) {
            if ((Type.MENU == type || Type.SINGLE == type) &&
                    (selectedIds == null || selectedIds.length != 1)) {
                throw new IllegalArgumentException();
            }
            ensureResult().putStringArray("choices", selectedIds);
            return super.confirm();
        }

        /**
         * Confirms this prompt with a single choice.
         *
         * @param selectedChoice The selected choice.
         *
         * @return A {@link PromptResponse} which can be used to complete
         *         the WPEResult associated with this prompt.
         */
        @UiThread
        public @NonNull PromptResponse confirm(@NonNull final Choice selectedChoice) {
            return confirm(selectedChoice == null ? null : selectedChoice.id);
        }

        /**
         * Confirms this prompt with multiple choices.
         *
         * @param selectedChoices The selected choices.
         *
         * @return A {@link PromptResponse} which can be used to complete
         *         the WPEResult associated with this prompt.
         */
        @UiThread
        public @NonNull PromptResponse confirm(@NonNull final Choice[] selectedChoices) {
            if ((Type.MENU == type || Type.SINGLE == type) &&
                    (selectedChoices == null || selectedChoices.length != 1)) {
                throw new IllegalArgumentException();
            }

            if (selectedChoices == null) {
                return confirm((String[]) null);
            }

            final String[] ids = new String[selectedChoices.length];
            for (int i = 0; i < ids.length; i++) {
                ids[i] = (selectedChoices[i] == null) ? null : selectedChoices[i].id;
            }

            return confirm(ids);
        }
    }

    /**
     * ColorPrompt contains the information necessary to represent a prompt for color
     * input generated by content.
     */
    public class ColorPrompt extends BasePrompt {
        /**
         * The default value supplied by content.
         */
        public final @Nullable String defaultValue;

        protected ColorPrompt(@Nullable final String title,
                              @Nullable final String defaultValue) {
            super(title);
            this.defaultValue = defaultValue;
        }

        /**
         * Confirms the prompt and passes the color value back to content.
         *
         * @param color A String representing the color to be returned to content.
         *
         * @return A {@link PromptResponse} which can be used to complete
         *         the WPEResult associated with this prompt.
         */
        @UiThread
        public @NonNull PromptResponse confirm(@NonNull final String color) {
            ensureResult().putString("color", color);
            return super.confirm();
        }
    }

    /**
     * DateTimePrompt contains the information necessary to represent a prompt for
     * date and/or time input generated by content.
     */
    public class DateTimePrompt extends BasePrompt {
        @Retention(RetentionPolicy.SOURCE)
        @IntDef({Type.DATE, Type.MONTH, Type.WEEK, Type.TIME, Type.DATETIME_LOCAL})
                /* package */ @interface DatetimeType {}

        public static class Type {
            /**
             * Prompt for year, month, and day.
             */
            public static final int DATE = 1;

            /**
             * Prompt for year and month.
             */
            public static final int MONTH = 2;

            /**
             * Prompt for year and week.
             */
            public static final int WEEK = 3;

            /**
             * Prompt for hour and minute.
             */
            public static final int TIME = 4;

            /**
             * Prompt for year, month, day, hour, and minute, without timezone.
             */
            public static final int DATETIME_LOCAL = 5;

            protected Type() {}
        }

        /**
         * One of {@link Type} indicating the type of prompt.
         */
        public final @DatetimeType int type;

        /**
         * A String representing the default value supplied by content.
         */
        public final @Nullable String defaultValue;

        /**
         * A String representing the minimum value allowed by content.
         */
        public final @Nullable String minValue;

        /**
         * A String representing the maximum value allowed by content.
         */
        public final @Nullable String maxValue;

        protected DateTimePrompt(@Nullable final String title,
                                 @DatetimeType final int type,
                                 @Nullable final String defaultValue,
                                 @Nullable final String minValue,
                                 @Nullable final String maxValue) {
            super(title);
            this.type = type;
            this.defaultValue = defaultValue;
            this.minValue = minValue;
            this.maxValue = maxValue;
        }

        /**
         * Confirms the prompt and passes the date and/or time value back to content.
         *
         * @param datetime A String representing the date and time to be returned to content.
         *
         * @return A {@link PromptResponse} which can be used to complete
         *         the WPEResult associated with this prompt.
         */
        @UiThread
        public @NonNull PromptResponse confirm(@NonNull final String datetime) {
            ensureResult().putString("datetime", datetime);
            return super.confirm();
        }
    }

    /**
     * FilePrompt contains the information necessary to represent a prompt for
     * a file or files generated by content.
     */
    public class FilePrompt extends BasePrompt {
        private static final String LOGTAG = "FilePrompt";

        @Retention(RetentionPolicy.SOURCE)
        @IntDef({Type.SINGLE, Type.MULTIPLE})
                /* package */ @interface FileType {}

        /**
         * Types of file prompts.
         */
        public static class Type {
            /**
             * Prompt for a single file.
             */
            public static final int SINGLE = 1;

            /**
             * Prompt for multiple files.
             */
            public static final int MULTIPLE = 2;

            protected Type() {}
        }

        @Retention(RetentionPolicy.SOURCE)
        @IntDef({Capture.NONE, Capture.ANY, Capture.USER, Capture.ENVIRONMENT})
                /* package */ @interface CaptureType {}

        /**
         * Possible capture attribute values.
         */
        public static class Capture {
            // These values should match the corresponding values in nsIFilePicker.idl
            /**
             * No capture attribute has been supplied by content.
             */
            public static final int NONE = 0;

            /**
             * The capture attribute was supplied with a missing or invalid value.
             */
            public static final int ANY = 1;

            /**
             * The "user" capture attribute has been supplied by content.
             */
            public static final int USER = 2;

            /**
             * The "environment" capture attribute has been supplied by content.
             */
            public static final int ENVIRONMENT = 3;

            protected Capture() {}
        }

        /**
         * One of {@link Type} indicating the prompt type.
         */
        public final @FileType int type;

        /**
         * An array of Strings giving the MIME types specified by the "accept" attribute,
         * if any are specified.
         */
        public final @Nullable String[] mimeTypes;

        /**
         * One of {@link Capture} indicating the capture attribute supplied by content.
         */
        public final @CaptureType int capture;

        protected FilePrompt(@Nullable final String title,
                             @FileType final int type,
                             @CaptureType final int capture,
                             @Nullable final String[] mimeTypes) {
            super(title);
            this.type = type;
            this.capture = capture;
            this.mimeTypes = mimeTypes;
        }

        /**
         * Confirms the prompt and passes the file URI back to content.
         *
         * @param context An Application context for parsing URIs.
         * @param uri The URI of the file chosen by the user.
         *
         * @return A {@link PromptResponse} which can be used to complete
         *         the WPEResult associated with this prompt.
         */
        @UiThread
        public @NonNull PromptResponse confirm(@NonNull final Context context,
                                               @NonNull final Uri uri) {
            return confirm(context, new Uri[] { uri });
        }

        /**
         * Confirms the prompt and passes the file URIs back to content.
         *
         * @param context An Application context for parsing URIs.
         * @param uris The URIs of the files chosen by the user.
         *
         * @return A {@link PromptResponse} which can be used to complete
         *         the WPEResult associated with this prompt.
         */
        @UiThread
        public @NonNull PromptResponse confirm(@NonNull final Context context,
                                               @NonNull final Uri[] uris) {
            if (Type.SINGLE == type && (uris == null || uris.length != 1)) {
                throw new IllegalArgumentException();
            }

            final String[] paths = new String[uris != null ? uris.length : 0];
            for (int i = 0; i < paths.length; i++) {
                paths[i] = getFile(context, uris[i]);
                if (paths[i] == null) {
                    Log.e(LOGTAG, "Only file URIs are supported: " + uris[i]);
                }
            }
            ensureResult().putStringArray("files", paths);

            return super.confirm();
        }

        private static String getFile(final @NonNull Context context, final @NonNull Uri uri) {
            if (uri == null) {
                return null;
            }
            if ("file".equals(uri.getScheme())) {
                return uri.getPath();
            }
            final ContentResolver cr = context.getContentResolver();
            final Cursor cur = cr.query(uri, new String[] { "_data" }, /* selection */ null,
                    /* args */ null, /* sort */ null);
            if (cur == null) {
                return null;
            }
            try {
                final int idx = cur.getColumnIndex("_data");
                if (idx < 0 || !cur.moveToFirst()) {
                    return null;
                }
                do {
                    try {
                        final String path = cur.getString(idx);
                        if (path != null && !path.isEmpty()) {
                            return path;
                        }
                    } catch (final Exception e) {
                    }
                } while (cur.moveToNext());
            } finally {
                cur.close();
            }
            return null;
        }
    }

    /**
     * PopupPrompt contains the information necessary to represent a popup blocking
     * request.
     */
    public class PopupPrompt extends BasePrompt {
        /**
         * The target URI for the popup; may be null.
         */
        public final @Nullable String targetUri;

        protected PopupPrompt(@Nullable final String targetUri) {
            super(null);
            this.targetUri = targetUri;
        }

        /**
         * Confirms the prompt and either allows or blocks the popup.
         *
         * @param response An {@link AllowOrDeny} specifying whether to allow or deny the popup.
         *
         * @return A {@link PromptResponse} which can be used to complete
         *         the WPEResult associated with this prompt.
         */
        @UiThread
        public @NonNull PromptResponse confirm(@NonNull final AllowOrDeny response) {
            boolean res = false;
            if (AllowOrDeny.ALLOW == response) {
                res = true;
            }
            ensureResult().putBoolean("response", res);
            return super.confirm();
        }
    }

    /**
     * SharePrompt contains the information necessary to represent a (v1) WebShare request.
     */
    public class SharePrompt extends BasePrompt {
        @Retention(RetentionPolicy.SOURCE)
        @IntDef({Result.SUCCESS, Result.FAILURE, Result.ABORT})
                /* package */ @interface ShareResult {}

        /**
         * Possible results to a {@link SharePrompt}.
         */
        public static class Result {
            /**
             * The user shared with another app successfully.
             */
            public static final int SUCCESS = 0;

            /**
             * The user attempted to share with another app, but it failed.
             */
            public static final int FAILURE = 1;

            /**
             * The user aborted the share.
             */
            public static final int ABORT = 2;

            protected Result() {}
        }

        /**
         * The text for the share request.
         */
        public final @Nullable String text;

        /**
         * The uri for the share request.
         */
        public final @Nullable String uri;

        protected SharePrompt(@Nullable final String title,
                              @Nullable final String text,
                              @Nullable final String uri) {
            super(title);
            this.text = text;
            this.uri = uri;
        }

        /**
         * Confirms the prompt and either blocks or allows the share request.
         *
         * @param response One of {@link Result} specifying the outcome of the
         *                 share attempt.
         *
         * @return A {@link PromptResponse} which can be used to complete the
         *         WPEResult associated with this prompt.
         */
        @UiThread
        public @NonNull PromptResponse confirm(@ShareResult final int response) {
            ensureResult().putInt("response", response);
            return super.confirm();
        }

        /**
         * Dismisses the prompt and returns {@link Result#ABORT} to web content.
         *
         * @return A {@link PromptResponse} which can be used to complete the
         *         WPEResult associated with this prompt.
         */
        @UiThread
        public @NonNull PromptResponse dismiss() {
            ensureResult().putInt("response", Result.ABORT);
            return super.dismiss();
        }
    }

    /**
     * Request containing information required to resolve Autocomplete
     * prompt requests.
     */
    public class AutocompleteRequest<T extends Autocomplete.Option<?>>
            extends BasePrompt {
        /**
         * The Autocomplete options for this request.
         * This can contain a single or multiple entries.
         */
        public final @NonNull T[] options;

        protected AutocompleteRequest(final @NonNull T[] options) {
            super(null);
            this.options = options;
        }

        /**
         * Confirm the request by responding with a selection.
         * See the PromptDelegate callbacks for specifics.
         *
         * @param selection The {@link Autocomplete.Option} used to confirm
         *                  the request.
         *
         * @return A {@link PromptResponse} which can be used to complete
         *         the WPEResult associated with this prompt.
         */
        @UiThread
        public @NonNull PromptResponse confirm(
                final @NonNull Autocomplete.Option<?> selection) {
            ensureResult().putBundle("selection", selection.toBundle());
            return super.confirm();
        }

        /**
         * Dismiss the request.
         * See the PromptDelegate callbacks for specifics.
         *
         * @return A {@link PromptResponse} which can be used to complete
         *         the WPEResult associated with this prompt.
         */
        @UiThread
        public @NonNull PromptResponse dismiss() {
            return super.dismiss();
        }
    }

    // Delegate functions.
    /**
     * Display an alert prompt.
     *
     * @param session Session that triggered the prompt.
     * @param prompt The {@link AlertPrompt} that describes the prompt.
     *
     * @return A WPEResult resolving to a {@link PromptResponse} which
     *         includes all necessary information to resolve the prompt.
     */
    @UiThread
    default @Nullable
    ResultAPI<PromptResponse> onAlertPrompt(@NonNull final SessionAPI session,
                                            @NonNull final AlertPrompt prompt) {
        return null;
    }

    /**
     * Display a onbeforeunload prompt.
     *
     * See https://developer.mozilla.org/en-US/docs/Web/API/WindowEventHandlers/onbeforeunload
     * See {@link BeforeUnloadPrompt}
     *
     * @param session Session that triggered the prompt
     * @param prompt the {@link BeforeUnloadPrompt} that describes the
     *               prompt.
     * @return A WPEResult resolving to {@link AllowOrDeny#ALLOW}
     *         if the page is allowed to continue with the navigation or
     *         {@link AllowOrDeny#DENY} otherwise.
     */
    @UiThread
    default @Nullable
    ResultAPI<PromptResponse> onBeforeUnloadPrompt(
            @NonNull final SessionAPI session,
            @NonNull final BeforeUnloadPrompt prompt
    ) {
        return null;
    }

    /**
     * Display a POST resubmission confirmation prompt.
     *
     * This prompt will trigger whenever refreshing or navigating to a page needs resubmitting
     * POST data that has been submitted already.
     *
     * @param session Session that triggered the prompt
     * @param prompt the {@link RepostConfirmPrompt} that describes the
     *               prompt.
     * @return A WPEResult resolving to {@link AllowOrDeny#ALLOW}
     *         if the page is allowed to continue with the navigation and resubmit the POST
     *         data or {@link AllowOrDeny#DENY} otherwise.
     */
    @UiThread
    default @Nullable
    ResultAPI<PromptResponse> onRepostConfirmPrompt(
            @NonNull final SessionAPI session,
            @NonNull final RepostConfirmPrompt prompt
    ) {
        return null;
    }

    /**
     * Display a button prompt.
     *
     * @param session Session that triggered the prompt.
     * @param prompt The {@link ButtonPrompt} that describes the prompt.
     *
     * @return A WPEResult resolving to a {@link PromptResponse} which
     *         includes all necessary information to resolve the prompt.
     */
    @UiThread
    default @Nullable
    ResultAPI<PromptResponse> onButtonPrompt(@NonNull final SessionAPI session,
                                             @NonNull final ButtonPrompt prompt) {
        return null;
    }

    /**
     * Display a text prompt.
     *
     * @param session Session that triggered the prompt.
     * @param prompt The {@link TextPrompt} that describes the prompt.
     *
     * @return A WPEResult resolving to a {@link PromptResponse} which
     *         includes all necessary information to resolve the prompt.
     */
    @UiThread
    default @Nullable
    ResultAPI<PromptResponse> onTextPrompt(@NonNull final SessionAPI session,
                                           @NonNull final TextPrompt prompt) {
        return null;
    }

    /**
     * Display an authorization prompt.
     *
     * @param session Session that triggered the prompt.
     * @param prompt The {@link AuthPrompt} that describes the prompt.
     *
     * @return A WPEResult resolving to a {@link PromptResponse} which
     *         includes all necessary information to resolve the prompt.
     */
    @UiThread
    default @Nullable
    ResultAPI<PromptResponse> onAuthPrompt(@NonNull final SessionAPI session,
                                           @NonNull final AuthPrompt prompt) {
        return null;
    }

    /**
     * Display a list/menu prompt.
     *
     * @param session Session that triggered the prompt.
     * @param prompt The {@link ChoicePrompt} that describes the prompt.
     *
     * @return A WPEResult resolving to a {@link PromptResponse} which
     *         includes all necessary information to resolve the prompt.
     */
    @UiThread
    default @Nullable
    ResultAPI<PromptResponse> onChoicePrompt(@NonNull final SessionAPI session,
                                             @NonNull final ChoicePrompt prompt) {
        return null;
    }

    /**
     * Display a color prompt.
     *
     * @param session Session that triggered the prompt.
     * @param prompt The {@link ColorPrompt} that describes the prompt.
     *
     * @return A WPEResult resolving to a {@link PromptResponse} which
     *         includes all necessary information to resolve the prompt.
     */
    @UiThread
    default @Nullable
    ResultAPI<PromptResponse> onColorPrompt(@NonNull final SessionAPI session,
                                            @NonNull final ColorPrompt prompt) {
        return null;
    }

    /**
     * Display a date/time prompt.
     *
     * @param session Session that triggered the prompt.
     * @param prompt The {@link DateTimePrompt} that describes the prompt.
     *
     * @return A WPEResult resolving to a {@link PromptResponse} which
     *         includes all necessary information to resolve the prompt.
     */
    @UiThread
    default @Nullable
    ResultAPI<PromptResponse> onDateTimePrompt(@NonNull final SessionAPI session,
                                               @NonNull final DateTimePrompt prompt) {
        return null;
    }

    /**
     * Display a file prompt.
     *
     * @param session Session that triggered the prompt.
     * @param prompt The {@link FilePrompt} that describes the prompt.
     *
     * @return A WPEResult resolving to a {@link PromptResponse} which
     *         includes all necessary information to resolve the prompt.
     */
    @UiThread
    default @Nullable
    ResultAPI<PromptResponse> onFilePrompt(@NonNull final SessionAPI session,
                                           @NonNull final FilePrompt prompt) {
        return null;
    }

    /**
     * Display a popup request prompt; this occurs when content attempts to open
     * a new window in a way that doesn't appear to be the result of user input.
     *
     * @param session Session that triggered the prompt.
     * @param prompt The {@link PopupPrompt} that describes the prompt.
     *
     * @return A WPEResult resolving to a {@link PromptResponse} which
     *         includes all necessary information to resolve the prompt.
     */
    @UiThread
    default @Nullable
    ResultAPI<PromptResponse> onPopupPrompt(@NonNull final SessionAPI session,
                                            @NonNull final PopupPrompt prompt) {
        return null;
    }

    /**
     * Display a share request prompt; this occurs when content attempts to use the
     * WebShare API.
     * See: https://developer.mozilla.org/en-US/docs/Web/API/Navigator/share
     *
     * @param session Session that triggered the prompt.
     * @param prompt The {@link SharePrompt} that describes the prompt.
     *
     * @return A WPEResult resolving to a {@link PromptResponse} which
     *         includes all necessary information to resolve the prompt.
     */
    @UiThread
    default @Nullable
    ResultAPI<PromptResponse> onSharePrompt(@NonNull final SessionAPI session,
                                            @NonNull final SharePrompt prompt) {
        return null;
    }

    /**
     * Handle a login save prompt request.
     * This is triggered by the user entering new or modified login
     * credentials into a login form.
     *
     * @param session The {@link Session} that triggered the request.
     * @param request The {@link AutocompleteRequest} containing the request
     *                details.
     *
     * @return A WPEResult resolving to a {@link PromptResponse}.
     *
     *         Confirm the request with an {@link Autocomplete.Option}
     *         to trigger a
     *         {@link Autocomplete.StorageDelegate#onLoginSave} request
     *         to save the given selection.
     *         The confirmed selection may be an entry out of the request's
     *         options, a modified option, or a freshly created login entry.
     *
     *         Dismiss the request to deny the saving request.
     */
    @UiThread
    default @Nullable
    ResultAPI<PromptResponse> onLoginSave(
            @NonNull final SessionAPI session,
            @NonNull final AutocompleteRequest<Autocomplete.LoginSaveOption>
                    request) {
        return null;
    }

    /**
     * Handle a login selection prompt request.
     * This is triggered by the user focusing on a login username field.
     *
     * @param session The {@link Session} that triggered the request.
     * @param request The {@link AutocompleteRequest} containing the request
     *                details.
     *
     * @return A WPEResult resolving to a {@link PromptResponse}
     *
     *         Confirm the request with an {@link Autocomplete.Option}
     *         to let GeckoView fill out the login forms with the given
     *         selection details.
     *         The confirmed selection may be an entry out of the request's
     *         options, a modified option, or a freshly created login entry.
     *
     *         Dismiss the request to deny autocompletion for the detected
     *         form.
     */
    @UiThread
    default @Nullable
    ResultAPI<PromptResponse> onLoginSelect(
            @NonNull final SessionAPI session,
            @NonNull final AutocompleteRequest<Autocomplete.LoginSelectOption>
                    request) {
        return null;
    }

    /**
     * Handle a credit card selection prompt request.
     * This is triggered by the user focusing on a credit card input field.
     *
     * @param session The {@link Session} that triggered the request.
     * @param request The {@link AutocompleteRequest} containing the request
     *                details.
     *
     * @return A WPEResult resolving to a {@link PromptResponse}
     *
     *         Confirm the request with an {@link Autocomplete.Option}
     *         to let GeckoView fill out the credit card forms with the given
     *         selection details.
     *         The confirmed selection may be an entry out of the request's
     *         options, a modified option, or a freshly created credit
     *         card entry.
     *
     *         Dismiss the request to deny autocompletion for the detected
     *         form.
     */
    @UiThread
    default @Nullable
    ResultAPI<PromptResponse> onCreditCardSelect(
            @NonNull final SessionAPI session,
            @NonNull final AutocompleteRequest<Autocomplete.CreditCardSelectOption>
                    request) {
        return null;
    }
}