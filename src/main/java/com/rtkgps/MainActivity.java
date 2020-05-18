package com.rtkgps;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.annotation.Nonnull;

import proguard.annotation.BuildConfig;

public abstract class MainActivity extends AppCompatActivity {

    private static final boolean DBG = BuildConfig.DEBUG & true;
    static final String TAG = MainActivity.class.getSimpleName();

    /**
     * The serialization (saved instance state) Bundle key representing the
     * current dropdown position.
     */
    public static final String RTKGPS_CHILD_DIRECTORY = "RtkGps/";

    RtkNaviService mRtkService;
    boolean mRtkServiceBound = false;
    protected String mSessionCode;
    protected static String mApplicationDirectory = "";

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PackageManager m = getPackageManager();
        String s = getPackageName();
        try {
            PackageInfo p = m.getPackageInfo(s, 0);
            MainActivity.mApplicationDirectory = p.applicationInfo.dataDir;
        } catch (NameNotFoundException e) {
            Log.w(TAG, "Error Package name not found ", e);
        }

        // copy assets/data
        try {
            copyAssetsToApplicationDirectory();
            copyAssetsToWorkingDirectory();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }




        if (savedInstanceState == null) {
            proxyIfUsbAttached(getIntent());
        }



    }

    @Nonnull
    public static File getLocalSocketPath(Context ctx, String socketName) {
        return ctx.getFileStreamPath(socketName);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mRtkServiceBound) {
            final Intent intent = new Intent(this, RtkNaviService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        proxyIfUsbAttached(intent);
    }



    @Override
    protected void onStop() {
        super.onStop();

        // Unbind from the service
        if (mRtkServiceBound) {
            unbindService(mConnection);
            mRtkServiceBound = false;
            mRtkService = null;
        }

    }




    protected void copyAssetsDirToApplicationDirectory(String sourceDir, File destDir) throws FileNotFoundException, IOException
    {
        //copy assets/data to appdir/data
        java.io.InputStream stream = null;
        java.io.OutputStream output = null;

        for(String fileName : this.getAssets().list(sourceDir))
        {
            stream = this.getAssets().open(sourceDir+File.separator + fileName);
            String dest = destDir+ File.separator + sourceDir + File.separator + fileName;
            File fdest = new File(dest);
            if (fdest.exists()) continue;

            File fpdestDir = new File(fdest.getParent());
            if ( !fpdestDir.exists() ) fpdestDir.mkdirs();

            output = new BufferedOutputStream(new FileOutputStream(dest));

            byte data[] = new byte[1024];
            int count;

            while((count = stream.read(data)) != -1)
            {
                output.write(data, 0, count);
            }

            output.flush();
            output.close();
            stream.close();

            stream = null;
            output = null;
        }
    }

    protected void copyAssetsToApplicationDirectory() throws FileNotFoundException, IOException
    {
       copyAssetsDirToApplicationDirectory("data",this.getFilesDir());
       copyAssetsDirToApplicationDirectory("proj4",this.getFilesDir());
    }

    protected void copyAssetsToWorkingDirectory() throws FileNotFoundException, IOException
    {
        copyAssetsDirToApplicationDirectory("ntripcaster",getFileStorageDirectory());
    }

    protected void proxyIfUsbAttached(Intent intent) {

        if (intent == null) return;

        if (!UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(intent.getAction())) return;

        if (DBG) Log.v(TAG, "usb device attached");

        final Intent proxyIntent = new Intent(UsbToRtklib.ACTION_USB_DEVICE_ATTACHED);
        proxyIntent.putExtras(intent.getExtras());
        sendBroadcast(proxyIntent);
    }



    public void startRtkService() {
        Log.d(TAG, "startRtkService");
        mSessionCode = String.valueOf(System.currentTimeMillis());
        final Intent rtkServiceIntent = new Intent(RtkNaviService.ACTION_START);
        rtkServiceIntent.putExtra(RtkNaviService.EXTRA_SESSION_CODE,mSessionCode);
        rtkServiceIntent.setClass(this, RtkNaviService.class);
        startService(rtkServiceIntent);
    }

    public String getSessionCode() {
        return mSessionCode;
    }

    private void stopRtkService() {
        Log.d(TAG, "stopRtkService");
        final Intent intent = new Intent(RtkNaviService.ACTION_STOP);
        intent.setClass(this, RtkNaviService.class);
        startService(intent);
    }

    public RtkNaviService getRtkService() {
        return mRtkService;
    }



    protected ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get
            // LocalService instance
            RtkNaviService.RtkNaviServiceBinder binder = (RtkNaviService.RtkNaviServiceBinder) service;
            mRtkService = binder.getService();
            mRtkServiceBound = true;
            invalidateOptionsMenu();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mRtkServiceBound = false;
            mRtkService = null;
            invalidateOptionsMenu();
        }
    };

    @Nonnull
    public static File getFileStorageDirectory() {
        File externalLocation = new File(Environment.getExternalStorageDirectory(), RTKGPS_CHILD_DIRECTORY);
        if(!externalLocation.isDirectory()) {
           if (externalLocation.mkdirs()) {
               Log.v(TAG, "Local storage created on external card");
           }else{
               externalLocation = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),RTKGPS_CHILD_DIRECTORY);
               if(!externalLocation.isDirectory()) {
                   if (externalLocation.mkdirs()) {
                       Log.v(TAG, "Local storage created on public storage");
                   }else{
                       externalLocation = new File(Environment.getDownloadCacheDirectory(), RTKGPS_CHILD_DIRECTORY);
                       if (!externalLocation.isDirectory()){
                           if (externalLocation.mkdirs()){
                               Log.v(TAG, "Local storage created on cache directory");
                           }else{
                               externalLocation = new File(Environment.getDataDirectory(),RTKGPS_CHILD_DIRECTORY);
                               if(!externalLocation.isDirectory()) {
                                   if (externalLocation.mkdirs()) {
                                       Log.v(TAG, "Local storage created on data storage");
                                   }else{
                                       Log.e(TAG,"NO WAY TO CREATE FILE SOTRAGE?????");
                                   }
                               }
                           }
                       }
                   }
               }
           }
        }
        return externalLocation;
    }


    public static String getAndCheckSessionDirectory(String code){
        String szSessionDirectory = MainActivity.getFileStorageDirectory() + File.separator + code;
        File fsessionDirectory = new File(szSessionDirectory);
        if (!fsessionDirectory.exists()){
            fsessionDirectory.mkdirs();
        }
        return szSessionDirectory;
    }





    public static String getApplicationDirectory() {
        return MainActivity.mApplicationDirectory;
    }



}
