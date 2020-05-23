package gpsplus.rtkgps.settings;

import android.content.Context;

import gpsplus.rtkgps.MainActivity;
import gpsplus.rtkgps.usb.SerialLineConfiguration;
import gpsplus.rtklib.constants.StreamFormat;
import gpsplus.rtklib.constants.StreamType;

import javax.annotation.Nonnull;

import gpsplus.rtklib.RtkServerSettings;

/**
 * Settings for usb-rover
 */
public class RoverSettings {

    public static final class UsbSettings implements RtkServerSettings.TransportSettings {

        private String mPath;

        private SerialLineConfiguration mSerialLineConfiguration;

        public UsbSettings() {
            mPath = "usb_streamName";
            mSerialLineConfiguration = new SerialLineConfiguration();
            mSerialLineConfiguration.setBaudrate(9600);

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

    public RtkServerSettings.InputStream getInputStream() {
        final RtkServerSettings.InputStream stream = new RtkServerSettings.InputStream();
        stream.setFormat(StreamFormat.UBX);
        stream.setTransportSettings(new RoverSettings.UsbSettings());
        return stream;
    }
}
