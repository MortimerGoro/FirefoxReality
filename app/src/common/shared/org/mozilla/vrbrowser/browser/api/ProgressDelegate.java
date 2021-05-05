package org.mozilla.vrbrowser.browser.api;

import android.os.Bundle;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import java.io.ByteArrayInputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public interface ProgressDelegate {
    /**
     * Class representing security information for a site.
     */
    public class SecurityInformation {
        private static final String LOGTAG = "ProgressDelegate";

        @Retention(RetentionPolicy.SOURCE)
        @IntDef({SECURITY_MODE_UNKNOWN, SECURITY_MODE_IDENTIFIED,
                SECURITY_MODE_VERIFIED})
                /* package */ @interface SecurityMode {}
        public static final int SECURITY_MODE_UNKNOWN = 0;
        public static final int SECURITY_MODE_IDENTIFIED = 1;
        public static final int SECURITY_MODE_VERIFIED = 2;

        @Retention(RetentionPolicy.SOURCE)
        @IntDef({CONTENT_UNKNOWN, CONTENT_BLOCKED, CONTENT_LOADED})
                /* package */ @interface ContentType {}
        public static final int CONTENT_UNKNOWN = 0;
        public static final int CONTENT_BLOCKED = 1;
        public static final int CONTENT_LOADED = 2;
        /**
         * Indicates whether or not the site is secure.
         */
        public final boolean isSecure;
        /**
         * Indicates whether or not the site is a security exception.
         */
        public final boolean isException;
        /**
         * Contains the origin of the certificate.
         */
        public final @Nullable String origin;
        /**
         * Contains the host associated with the certificate.
         */
        public final @NonNull String host;

        /**
         * The server certificate in use, if any.
         */
        public final @Nullable X509Certificate certificate;

        /**
         * Indicates the security level of the site; possible values are SECURITY_MODE_UNKNOWN,
         * SECURITY_MODE_IDENTIFIED, and SECURITY_MODE_VERIFIED. SECURITY_MODE_IDENTIFIED
         * indicates domain validation only, while SECURITY_MODE_VERIFIED indicates extended validation.
         */
        public final @SecurityMode int securityMode;
        /**
         * Indicates the presence of passive mixed content; possible values are
         * CONTENT_UNKNOWN, CONTENT_BLOCKED, and CONTENT_LOADED.
         */
        public final @ContentType int mixedModePassive;
        /**
         * Indicates the presence of active mixed content; possible values are
         * CONTENT_UNKNOWN, CONTENT_BLOCKED, and CONTENT_LOADED.
         */
        public final @ContentType int mixedModeActive;

        /* package */ SecurityInformation(final Bundle identityData) {
            final Bundle mode = identityData.getBundle("mode");

            mixedModePassive = mode.getInt("mixed_display");
            mixedModeActive = mode.getInt("mixed_active");

            securityMode = mode.getInt("identity");

            isSecure = identityData.getBoolean("secure");
            isException = identityData.getBoolean("securityException");
            origin = identityData.getString("origin");
            host = identityData.getString("host");

            X509Certificate decodedCert = null;
            try {
                final CertificateFactory factory = CertificateFactory.getInstance("X.509");
                final String certString = identityData.getString("certificate");
                if (certString != null) {
                    final byte[] certBytes = Base64.decode(certString, Base64.NO_WRAP);
                    decodedCert = (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(certBytes));
                }
            } catch (final CertificateException e) {
                Log.e(LOGTAG, "Failed to decode certificate", e);
            }

            certificate = decodedCert;
        }

        /**
         * Empty constructor for tests
         */
        protected SecurityInformation() {
            mixedModePassive = 0;
            mixedModeActive = 0;
            securityMode = 0;
            isSecure = false;
            isException = false;
            origin = "";
            host = "";
            certificate = null;
        }
    }

    /**
     * A View has started loading content from the network.
     * @param session Session that initiated the callback.
     * @param url The resource being loaded.
     */
    @UiThread
    default void onPageStart(@NonNull final SessionAPI session, @NonNull final String url) {}

    /**
     * A View has finished loading content from the network.
     * @param session Session that initiated the callback.
     * @param success Whether the page loaded successfully or an error occurred.
     */
    @UiThread
    default void onPageStop(@NonNull final SessionAPI session, final boolean success) {}

    /**
     * Page loading has progressed.
     * @param session Session that initiated the callback.
     * @param progress Current page load progress value [0, 100].
     */
    @UiThread
    default void onProgressChange(@NonNull final SessionAPI session, final int progress) {}

    /**
     * The security status has been updated.
     * @param session Session that initiated the callback.
     * @param securityInfo The new security information.
     */
    @UiThread
    default void onSecurityChange(@NonNull final SessionAPI session,
                                  @NonNull final SecurityInformation securityInfo) {}

    /**
     * The browser session state has changed. This can happen in response to
     * navigation, scrolling, or form data changes; the session state passed
     * includes the most up to date information on all of these.
     * @param session Session that initiated the callback.
     * @param sessionState SessionState representing the latest browser state.
     */
    @UiThread
    default void onSessionStateChange(@NonNull final SessionAPI session,
                                      @NonNull final SessionStateAPI sessionState) {}
}