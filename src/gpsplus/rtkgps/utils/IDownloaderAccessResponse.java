package gpsplus.rtkgps.utils;

public interface IDownloaderAccessResponse {
    void postResult(String asyncresult, PreciseEphemerisDownloader.DownloaderCaller caller);
}