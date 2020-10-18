@file:Suppress("PackageName", "unused")

package com.afsar.customflix.Service

import android.annotation.SuppressLint
import android.app.Notification
import android.util.Log
import com.afsar.customflix.R
import com.afsar.customflix.Utilities.CFApplication
import com.google.android.exoplayer2.offline.Download
import com.google.android.exoplayer2.offline.DownloadManager
import com.google.android.exoplayer2.offline.DownloadService
import com.google.android.exoplayer2.scheduler.PlatformScheduler
import com.google.android.exoplayer2.ui.DownloadNotificationHelper
import com.google.android.exoplayer2.util.NotificationUtil
import com.google.android.exoplayer2.util.Util

/**
 * A service for downloading media.
 */
class DemoDownloadService : DownloadService(
    FOREGROUND_NOTIFICATION_ID,
    DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
    CHANNEL_ID,
    R.string.exo_download_notification_channel_name,  /* channelDescriptionResourceId= */
    0
) {
    var platformScheduler: PlatformScheduler? = null
    private var notificationHelper: DownloadNotificationHelper? = null
    override fun onCreate() {
        super.onCreate()
        notificationHelper = DownloadNotificationHelper(this, CHANNEL_ID)
    }

    override fun getDownloadManager(): DownloadManager {
        return (application as CFApplication).getDownloadManager()!!
    }

    @SuppressLint("ObsoleteSdkInt")
    override fun getScheduler(): PlatformScheduler? {
        Log.d("Scheduler", "Called")
        return if (Util.SDK_INT >= 21) PlatformScheduler(this, JOB_ID) else null
    }

    override fun getForegroundNotification(downloads: List<Download>): Notification {
        //float downloadPercentage = downloads.get(0).getPercentDownloaded();
        //Log.d("download",":::: " + downloadPercentage);
        return notificationHelper!!.buildProgressNotification(
            R.drawable.customflix,  /* contentIntent= */null,  /* message= */null, downloads
        )
    }

    override fun onDownloadChanged(download: Download) {
        Log.d("download", "changed")
        val notification: Notification = when (download.state) {
            Download.STATE_COMPLETED -> {
                notificationHelper!!.buildDownloadCompletedNotification(
                    R.drawable.videodownload,  /* contentIntent= */
                    null,
                    Util.fromUtf8Bytes(download.request.data)
                )
            }
            Download.STATE_FAILED -> {
                notificationHelper!!.buildDownloadFailedNotification(
                    R.drawable.canceldownload,  /* contentIntent= */
                    null,
                    Util.fromUtf8Bytes(download.request.data)
                )
            }
            else -> {
                return
            }
        }
        NotificationUtil.setNotification(this, nextNotificationId++, notification)
    }

    companion object {
        private const val CHANNEL_ID = "download_channel"
        private const val JOB_ID = 1
        private const val FOREGROUND_NOTIFICATION_ID = 1
        var DownloadProgress = 0f
        private var nextNotificationId = FOREGROUND_NOTIFICATION_ID + 1
    }

    init {
        nextNotificationId = FOREGROUND_NOTIFICATION_ID + 1
    }
}
