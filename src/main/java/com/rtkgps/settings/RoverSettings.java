package com.rtkgps.settings;

import android.content.Context;

import com.rtkgps.MainActivity;
import com.rtkgps.usb.SerialLineConfiguration;
import com.rtklib.RtkServerSettings.*;
import com.rtklib.constants.StreamType;

import javax.annotation.Nonnull;

/**
 * Settings for usb-rover
 */
public class RoverSettings {

    public static final class UsbSettings implements TransportSettings {

        private String mPath;

        private SerialLineConfiguration mSerialLineConfiguration;

        public UsbSettings() {
            mPath = null;
            mSerialLineConfiguration = new SerialLineConfiguration();
        }

        @Override
        public StreamType getType() {
            return StreamType.USB;
        }

        @Override
        public String getPath() {
            if (mPath == null) throw new IllegalStateException("Path not initialized. Call updatePath()");
            return mPath;
        }

        public void updatePath(Context context, String sharedPrefsName) {
            mPath = MainActivity.getLocalSocketPath(context,
                    usbLocalSocketName(sharedPrefsName)).getAbsolutePath();
        }


        @Nonnull
        public static String usbLocalSocketName(String stream) {
            return "usb_" + stream; // + "_" + address.replaceAll("\\W", "_");
        }

        public UsbSettings setSerialLineConfiguration(SerialLineConfiguration conf) {
            mSerialLineConfiguration.set(conf);
            return this;
        }

        public SerialLineConfiguration getSerialLineConfiguration() {
            return new SerialLineConfiguration(mSerialLineConfiguration);
        }

        @Override
        public UsbSettings copy() {
            UsbSettings v = new UsbSettings();
            v.mSerialLineConfiguration.set(mSerialLineConfiguration);
            v.mPath = mPath;
            return v;
        }
    }

    public InputStream getInputStream() {

    }
}
