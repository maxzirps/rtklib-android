package com.rtkgps.settings;

import com.rtkgps.MainActivity;
import com.rtklib.RtkServerSettings;
import com.rtklib.SolutionOptions;
import com.rtklib.constants.SolutionFormat;
import com.rtklib.constants.StreamType;

import java.io.File;

import javax.annotation.Nonnull;

/**
 * Settings for storing output to file
 */
public class OutputSettings {

    public static final class Value implements RtkServerSettings.TransportSettings {
        private String filename;

        public static final String DEFAULT_FILENAME = "solution_%Y%m%d%h%M%S.pos";

        public Value() {
            filename = DEFAULT_FILENAME;
        }

        public Value setFilename(@Nonnull String filename) {
            if (filename == null) throw new NullPointerException();
            this.filename = filename;
            return this;
        }

        @Override
        public StreamType getType() {
            return StreamType.FILE;
        }

        @Override
        public String getPath() {
            return (new File(MainActivity.getFileStorageDirectory(), filename)).getAbsolutePath();
        }

        @Override
        public Value copy() {
            return new Value().setFilename(filename);
        }
    }

    public RtkServerSettings.OutputStream getOutputStream(SolutionOptions solOptsBase) {
        final RtkServerSettings.OutputStream stream = new RtkServerSettings.OutputStream();
        stream
                .setSolutionOptions(solOptsBase)
                .setSolutionFormat(SolutionFormat.NMEA)
                .setTransportSettings(new OutputSettings.Value());
        return stream;
    }
}
