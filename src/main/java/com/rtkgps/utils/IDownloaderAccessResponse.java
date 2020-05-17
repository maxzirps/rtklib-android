package com.rtkgps.utils;

import com.rtkgps.ToolsActivity.DownloaderCaller;

public interface IDownloaderAccessResponse {
    void postResult(String asyncresult, DownloaderCaller caller);
}