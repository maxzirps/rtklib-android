package com.rtkgps;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.rtkgps.settings.RoverSettings;
import com.rtkgps.settings.SettingsHelper;
import com.rtkgps.utils.GpsTime;
import com.rtkgps.utils.PreciseEphemerisDownloader;
import com.rtkgps.utils.PreciseEphemerisProvider;
import com.rtkgps.utils.Shapefile;
import com.rtklib.BuildConfig;
import com.rtklib.R;
import com.rtklib.RtkCommon;
import com.rtklib.RtkCommon.Position3d;
import com.rtklib.RtkControlResult;
import com.rtklib.RtkServer;
import com.rtklib.RtkServerObservationStatus;
import com.rtklib.RtkServerSettings;
import com.rtklib.RtkServerSettings.TransportSettings;
import com.rtklib.RtkServerStreamStatus;
import com.rtklib.Solution;
import com.rtklib.constants.EphemerisOption;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;

public class RtkNaviService extends IntentService {

    private String m_pointName = "POINT";


    public RtkNaviService() {
        super(RtkNaviService.class.getSimpleName());
        // TODO Auto-generated constructor stub
    }

    @SuppressWarnings("unused")
    private static final boolean DBG = BuildConfig.DEBUG & true;
    static final String TAG = RtkNaviService.class.getSimpleName();

    public static final String ACTION_START = "com.rtkgps.RtkNaviService.START";
    public static final String ACTION_STOP = "com.rtkgps.RtkNaviService.STOP";
    public static final String ACTION_STORE_POINT = "com.rtkgps.RtkNaviService.STORE_POINT";
    public static final String EXTRA_SESSION_CODE = "com.rtkgps.RtkNaviService.SESSION_CODE";
    public static final String EXTRA_POINT_NAME = "com.rtkgps.RtkNaviService.POINT_NAME";
    private static final String MM_MAP_HEADER = "COMPD_CS[\"WGS 84\",GEOGCS[\"\",DATUM[\"WGS 84\",SPHEROID[\"WGS 84\",6378137,298.257223563],TOWGS84[0,0,0,0,0,0,0]],PRIMEM[\"Greenwich\",0],UNIT[\"Degrees\",0.0174532925199433],AXIS[\"Long\",East],AXIS[\"Lat\",North]],VERT_CS[\"\",VERT_DATUM[\"Ellipsoid\",2002],UNIT[\"Meters\",1],AXIS[\"Height\",Up]]]\r\n";
    private boolean mHavePoint = false;

    // Binder given to clients
    private final IBinder mBinder = new RtkNaviServiceBinder();

    public static final RtkServer mRtkServer = new RtkServer();

    public static boolean mbStarted = false;
    private PowerManager.WakeLock mCpuLock;

    private UsbToRtklib mUsbReceiver;
    private String mSessionCode;
    private Shapefile mShapefile;

    @Override
    public void onCreate() {
        super.onCreate();

        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mCpuLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            Log.v(TAG, "RtkNaviService restarted");
            processStart();
        } else {
            final String action = intent.getAction();

            if (action.equals(ACTION_START)) {
                if (intent.hasExtra(EXTRA_SESSION_CODE)) {
                    mSessionCode = intent.getStringExtra(EXTRA_SESSION_CODE);
                } else {
                    mSessionCode = String.valueOf(System.currentTimeMillis());
                }
                mShapefile = new Shapefile(MainActivity.getAndCheckSessionDirectory(mSessionCode),
                        mSessionCode + ".shp");
                processStart();
            } else if (action.equals(ACTION_STOP)) {
                processStop();
                mShapefile.close();
                if (mHavePoint) {
                    createMapFile();
                }
            } else if (action.equals(ACTION_STORE_POINT)) {
                if (intent.hasExtra(EXTRA_POINT_NAME)) {
                    m_pointName = intent.getStringExtra(EXTRA_POINT_NAME);
                }
                addPointToCRW(m_pointName);
            } else Log.e(TAG, "onStartCommand(): unknown action " + action);
        }
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }


    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return mBinder;
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

    }


    public final RtkServerStreamStatus getStreamStatus(
            RtkServerStreamStatus status) {
        return mRtkServer.getStreamStatus(status);
    }

    public final RtkServerObservationStatus getRoverObservationStatus(
            RtkServerObservationStatus status) {

        return mRtkServer.getRoverObservationStatus(status);
    }

    public final RtkServerObservationStatus getBaseObservationStatus(
            RtkServerObservationStatus status) {
        return mRtkServer.getBaseObservationStatus(status);
    }

    public RtkControlResult getRtkStatus(RtkControlResult dst) {
        return mRtkServer.getRtkStatus(dst);
    }

    public static void loadSP3(String file) {
        if (mRtkServer != null)
            mRtkServer.readSP3(file);
    }

    public static void loadSatAnt(String file) {
        if (mRtkServer != null)
            mRtkServer.readSatAnt(file);
    }

    public boolean isServiceStarted() {
        return mRtkServer.getStatus() != RtkServerStreamStatus.STATE_CLOSE;
    }

    public int getServerStatus() {
        return mRtkServer.getStatus();
    }

    public Solution[] readSolutionBuffer() {
        return mRtkServer.readSolutionBuffer();
    }

    private void createMapFile() {
        try {
            String sessionDirectory = MainActivity.getAndCheckSessionDirectory(mSessionCode);
            File mapFile = new File(sessionDirectory + File.separator + mSessionCode + ".map\r\n");
            if (!mapFile.exists()) {
                mapFile.createNewFile();
            }
            FileWriter mapFileWriter = new FileWriter(mapFile, true);
            BufferedWriter out = new BufferedWriter(mapFileWriter);
            out.write(MM_MAP_HEADER + "\r\n");
            out.write(mSessionCode + ".shp\r\n");
            out.close();
            mapFileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void addPointToCRW(String pointName) {
        double lat, lon, height, dLat, dLon;
        double Qe[] = new double[9];
        Position3d roverPos;
        int nbSat = 0;

        GpsTime gpsTime = new GpsTime();
        gpsTime.setTime(System.currentTimeMillis());


        Solution[] currentSolutions = readSolutionBuffer();
        Solution currentSolution = currentSolutions[currentSolutions.length - 1];
        RtkCommon.Matrix3x3 cov = currentSolution.getQrMatrix();
        Position3d roverEcefPos = currentSolution.getPosition();
        roverPos = RtkCommon.ecef2pos(roverEcefPos);
        lat = roverPos.getLat();
        lon = roverPos.getLon();
        dLat = Math.toDegrees(lat);
        dLon = Math.toDegrees(lon);
        Qe = RtkCommon.covenu(lat, lon, cov).getValues();
        nbSat = currentSolution.getNs();
        height = roverPos.getHeight();
        String currentLine = String.format(Locale.ROOT, "%s,%s,%.6f,%.6f,%.3f,%d,%.3f,%.3f,%.1f\n", gpsTime.getStringGpsWeek(), gpsTime.getStringGpsTOW(), dLon, dLat, height, 10, 0D, 0D, 0D);
        try {
            String szSessionDirectory = MainActivity.getAndCheckSessionDirectory(mSessionCode);
            File crwFile = new File(szSessionDirectory + File.separator + mSessionCode + ".crw");
            if (!crwFile.exists()) {
                crwFile.createNewFile();
            }
            FileWriter crwFileWriter = new FileWriter(crwFile, true);
            BufferedWriter out = new BufferedWriter(crwFileWriter);
            out.write(currentLine);
            out.close();
            crwFileWriter.close();
            mHavePoint = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (mShapefile != null) {
            mShapefile.addPoint(pointName, dLon, dLat, height,
                    Math.sqrt(Qe[4] < 0 ? 0 : Qe[4]),
                    Math.sqrt(Qe[0] < 0 ? 0 : Qe[0]),
                    Math.sqrt(Qe[8] < 0 ? 0 : Qe[8]),
                    gpsTime.getGpsWeek(), (long) gpsTime.getSecondsOfWeek(),
                    nbSat);
        }
    }

    /**
     * Class used for the client Binder. Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class RtkNaviServiceBinder extends Binder {
        public RtkNaviService getService() {
            // Return this instance of UpdateDbService so clients can call
            // public methods
            return RtkNaviService.this;
        }
    }

    public void processStart() {
        final RtkServerSettings settings;

        mbStarted = true;

        if (isServiceStarted()) return;

        settings = SettingsHelper.loadSettings(this);

        mRtkServer.setServerSettings(settings);

        if (!mRtkServer.start()) {
            Log.e(TAG, "rtkSrvStart() error");
            return;
        }

        EphemerisOption ephemerisOption = EphemerisOption.BRDC;
        PreciseEphemerisProvider provider = ephemerisOption.getProvider();
        if (provider != null) {
            if (PreciseEphemerisDownloader.isCurrentOrbitsPresent(provider)) {
                loadSP3(PreciseEphemerisDownloader.getCurrentOrbitFile(provider).getAbsolutePath());
            }
        }

        startUsb();

        mCpuLock.acquire();


        //load satellite antennas
        loadSatAnt(MainActivity.getApplicationDirectory() + File.separator + "files" + File.separator + "data" + File.separator + "igs05.atx");
    }


    private void processStop() {
        mbStarted = false;
        stop();
        stopSelf();
    }


    private void stop() {
        stopForeground(true);
        if (mCpuLock.isHeld()) mCpuLock.release();

        if (isServiceStarted()) {
            mRtkServer.stop();

            stopUsb();
            // Tell the user we stopped.
            Toast.makeText(this, R.string.local_service_stopped, Toast.LENGTH_SHORT)
                    .show();
        }
    }

    @Override
    public void onDestroy() {
        stop();
    }


    private class UsbCallbacks implements UsbToRtklib.Callbacks {

        private int mStreamId;
        private final Handler mHandler;

        public UsbCallbacks(int streamId) {
            mStreamId = streamId;
            mHandler = new Handler();
        }

        @Override
        public void onConnected() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(RtkNaviService.this, R.string.usb_connected,
                            Toast.LENGTH_SHORT).show();
                }
            });

            new Thread() {
                @Override
                public void run() {
                    mRtkServer.sendStartupCommands(mStreamId);
                }
            }.run();

        }

        @Override
        public void onStopped() {
        }

        @Override
        public void onConnectionLost() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(RtkNaviService.this, R.string.usb_connection_lost,
                            Toast.LENGTH_SHORT).show();
                }
            });
        }

    }


    private void startUsb() {
        RtkServerSettings settings = mRtkServer.getServerSettings();

        {
            final TransportSettings roverSettings = settings.getInputRover().getTransportSettings();
            RoverSettings.UsbSettings usbSettings = (com.rtkgps.settings.RoverSettings.UsbSettings) roverSettings;
            mUsbReceiver = new UsbToRtklib(this, usbSettings.getPath());
            mUsbReceiver.setSerialLineConfiguration(usbSettings.getSerialLineConfiguration());
            mUsbReceiver.setCallbacks(new UsbCallbacks(RtkServer.RECEIVER_ROVER));
            mUsbReceiver.start();
            return;
        }

    }

    private void stopUsb() {
        if (mUsbReceiver != null) {
            mUsbReceiver.stop();
            mUsbReceiver = null;
        }
    }


}
