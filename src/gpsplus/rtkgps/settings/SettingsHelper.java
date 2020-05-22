package gpsplus.rtkgps.settings;


import android.content.Context;

import gpsplus.rtklib.ProcessingOptions;
import gpsplus.rtklib.RtkServerSettings;
import gpsplus.rtklib.SolutionOptions;

/**
 * Loads all settings; normally you don't need to change this file
 */
public class SettingsHelper {

    public static RtkServerSettings loadSettings(Context ctx) {
        final RtkServerSettings settings;
        ProcessingOptions procOpts;
        SolutionOptions solOptsBase;

        settings = new RtkServerSettings();

        procOpts = new ProcessingSettings().getProcessingOptions();
        settings.setProcessingOptions(procOpts);

        solOptsBase = new SolutionSettings().getSolutionOptions();

        RtkServerSettings.InputStream rover = new RoverSettings().getInputStream();
        RtkServerSettings.InputStream base = new BaseSettings().getInputStream();
        RtkServerSettings.OutputStream output = new OutputSettings().getOutputStream(solOptsBase);

        settings
                .setInputRover(rover)
                .setInputBase(base)
                .setOutputSolution1(output);

        return settings;
    }
}
