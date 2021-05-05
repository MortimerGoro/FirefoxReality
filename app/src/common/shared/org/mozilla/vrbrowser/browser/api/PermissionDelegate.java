package org.mozilla.vrbrowser.browser.api;

import org.mozilla.vrbrowser.browser.engine.Session;

public interface PermissionDelegate {
    public interface Callback {
        void grant();
        void reject();
    }

    /**
     * Permission for using the geolocation API.
     * See: https://developer.mozilla.org/en-US/docs/Web/API/Geolocation
     */
    int PERMISSION_GEOLOCATION = 0;

    /**
     * Permission for using the notifications API.
     * See: https://developer.mozilla.org/en-US/docs/Web/API/notification
     */
    int PERMISSION_DESKTOP_NOTIFICATION = 1;

    /**
     * Permission for using the storage API.
     * See: https://developer.mozilla.org/en-US/docs/Web/API/Storage_API
     */
    int PERMISSION_PERSISTENT_STORAGE = 2;

    /**
     * Permission for using the WebXR API.
     * See: https://www.w3.org/TR/webxr
     */
    int PERMISSION_XR = 3;

    /**
     * Permission for allowing autoplay of inaudible (silent) video.
     */
    int PERMISSION_AUTOPLAY_INAUDIBLE = 4;

    /**
     * Permission for allowing autoplay of audible video.
     */
    int PERMISSION_AUTOPLAY_AUDIBLE = 5;

    /**
     * Permission for accessing system media keys used to decode DRM media.
     */
    int PERMISSION_MEDIA_KEY_SYSTEM_ACCESS = 6;

    public void onAndroidPermissionsRequest(SessionAPI aSession, String[] permissions, Callback aCallback);
    public void onContentPermissionRequest(SessionAPI aSession, String aUri, int aType, Callback callback);
    public void onMediaPermissionRequest(SessionAPI aSession, String aUri, String[] aVideo, String[] aAudio, Callback aMediaCallback);
}
