package com.rtkgps.settings;


import android.text.TextUtils;

import com.rtklib.RtkServerSettings;
import com.rtklib.constants.StreamFormat;
import com.rtklib.constants.StreamType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Settings for ntrip-caster-base
 */
public class BaseSettings {

    public static final class Value implements RtkServerSettings.TransportSettings, Cloneable {
        private @Nonnull
        String host;
        private int port;
        private @Nonnull
        String mountpoint;
        private @Nonnull
        String user;
        private @Nonnull
        String password;

        public static final String DEFAULT_HOST = "69.75.31.235";
        public static final int DEFAULT_PORT = 2101;
        public static final String DEFAULT_MOUNTPOUNT = "w-agrar";
        public static final String DEFAULT_USER = "no-user";
        public static final String DEFAULT_PASSWORD = "no-password";

        public Value() {
            host = DEFAULT_HOST;
            port = DEFAULT_PORT;
            mountpoint = DEFAULT_MOUNTPOUNT;
            user = DEFAULT_USER;
            password = DEFAULT_PASSWORD;
        }


        @Override
        public StreamType getType() {
            return StreamType.NTRIPCLI;
        }

        @Override
        public String getPath() {
            return createNtripPath(user, password, host,
                    String.valueOf(port), mountpoint, null);
        }


        @Override
        protected Value clone() {
            try {
                return (Value) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public Value copy() {
            return clone();
        }

        private String createNtripPath(@Nullable String user,
                                       @Nullable String passwd,
                                       @Nullable String host,
                                       @Nullable String port,
                                       @Nullable String mountpoint,
                                       @Nullable String str) {
            StringBuilder path;
            path = new StringBuilder();

            final boolean emptyUser, emptyPasswd;

            emptyUser = TextUtils.isEmpty(user);
            emptyPasswd = TextUtils.isEmpty(passwd);

            if (!emptyUser) {
                path.append(user.replaceAll(":", "")); //All characters except semicolon
            }

            if (!emptyPasswd) {
                if (!emptyUser) path.append(':');
                path.append(passwd.replaceAll(":", "")); //All characters except semicolon);
            }

            if (!emptyUser || !emptyPasswd) {
                path.append('@');
            }

            if (TextUtils.isEmpty(host)) host = "localhost";

            path.append(host);
            if (!TextUtils.isEmpty(port)) {
                path.append(':').append(port);
            }

            path.append('/');

            if (!TextUtils.isEmpty(mountpoint)) path.append(mountpoint);

            if (!TextUtils.isEmpty(str)) {
                path.append(':').append(str);
            }

            return path.toString();
        }
    }


    public RtkServerSettings.InputStream getInputStream() {
        final RtkServerSettings.InputStream stream = new RtkServerSettings.InputStream();
        stream.setFormat(StreamFormat.RTCM3);
        stream.setTransportSettings(new BaseSettings.Value());
        return stream;
    }

}
