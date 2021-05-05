package org.mozilla.vrbrowser.browser.api;
import androidx.annotation.AnyThread;

/**
 * This represents a decision to allow or deny a request.
 */
@AnyThread
public enum AllowOrDeny {
    ALLOW, DENY;
}