package org.mozilla.vrbrowser.browser.api;

import android.os.Bundle;

import androidx.annotation.LongDef;
import androidx.annotation.NonNull;
import androidx.annotation.UiThread;

import org.mozilla.vrbrowser.browser.engine.Session;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Session applications implement this interface to handle media events.
 *
 */
public interface MediaDelegate {

    class RecordingDevice {

        /*
         * Default status flags for this RecordingDevice.
         */
        public static class Status {
            public static final long RECORDING = 0;
            public static final long INACTIVE = 1 << 0;

            // Do not instantiate this class.
            protected Status() {}
        }

        /*
         * Default device types for this RecordingDevice.
         */
        public static class Type {
            public static final long CAMERA = 0;
            public static final long MICROPHONE = 1 << 0;

            // Do not instantiate this class.
            protected Type() {}
        }

        @Retention(RetentionPolicy.SOURCE)
        @LongDef(flag = true,
                value = { Status.RECORDING, Status.INACTIVE })
                /* package */ @interface RecordingStatus {}

        @Retention(RetentionPolicy.SOURCE)
        @LongDef(flag = true,
                value = {Type.CAMERA, Type.MICROPHONE})
                /* package */ @interface DeviceType {}

        /**
         * A long giving the current recording status, must be either Status.RECORDING,
         * Status.PAUSED or Status.INACTIVE.
         */
        public final @RecordingStatus long status;

        /**
         * A long giving the type of the recording device, must be either Type.CAMERA or Type.MICROPHONE.
         */
        public final @DeviceType long type;

        private static @DeviceType long getTypeFromString(final String type) {
            if ("microphone".equals(type)) {
                return Type.MICROPHONE;
            } else if ("camera".equals(type)) {
                return Type.CAMERA;
            } else {
                throw new IllegalArgumentException("String: " + type + " is not a valid recording device string");
            }
        }

        private static @RecordingStatus long getStatusFromString(final String type) {
            if ("recording".equals(type)) {
                return Status.RECORDING;
            } else {
                return Status.INACTIVE;
            }
        }

        /* package */ RecordingDevice(final Bundle media) {
            status = getStatusFromString(media.getString("status"));
            type = getTypeFromString(media.getString("type"));
        }

        /**
         * Empty constructor for tests.
         */
        protected RecordingDevice() {
            status = Status.INACTIVE;
            type = Type.CAMERA;
        }
    }
    /**
     * An HTMLMediaElement has been created.
     * @param session Session instance.
     * @param element The media element that was just created.
     *
     */
    @UiThread
    default void onMediaAdd(@NonNull final SessionAPI session, @NonNull final MediaElement element) {}

    /**
     * An HTMLMediaElement has been unloaded.
     * @param session Session instance.
     * @param element The media element that was unloaded.
     *
     */
    @UiThread
    default void onMediaRemove(@NonNull final SessionAPI session, @NonNull final MediaElement element) {}

    /**
     * A recording device has changed state.
     * Any change to the recording state of the devices microphone or camera will call this
     * delegate method. The argument provides details of the active recording devices.
     * @param session The session that the event has originated from.
     * @param devices The list of active devices and their recording state.
     */
    @UiThread
    default void onRecordingStatusChanged(@NonNull final SessionAPI session, @NonNull final RecordingDevice[] devices) {}
}