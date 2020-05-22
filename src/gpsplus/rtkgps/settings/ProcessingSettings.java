package gpsplus.rtkgps.settings;

import gpsplus.rtklib.ProcessingOptions;
import gpsplus.rtklib.constants.NavigationSystem;
import gpsplus.rtklib.constants.PositioningMode;

import java.util.HashSet;
import java.util.Set;

public class ProcessingSettings {

    public ProcessingOptions getProcessingOptions () {
        final ProcessingOptions processingOptions = new ProcessingOptions();
        Set<NavigationSystem> navigationSystems = new HashSet<NavigationSystem>();
        navigationSystems.add(NavigationSystem.GLO);
        navigationSystems.add(NavigationSystem.GAL);
        navigationSystems.add(NavigationSystem.GPS);
        processingOptions.setPositioningMode(PositioningMode.KINEMA);
        processingOptions.setNavigationSystem(navigationSystems);
        processingOptions.setNumberOfFrequencies(2);
        return processingOptions;
    }
}
