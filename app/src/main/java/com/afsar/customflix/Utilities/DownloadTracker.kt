@file:Suppress("PackageName", "unused", "MemberVisibilityCanBePrivate", "UNUSED_VARIABLE")

package com.afsar.customflix.Utilities

/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.Context
import android.content.DialogInterface
import android.net.Uri
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import com.afsar.customflix.Service.DemoDownloadService
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.RenderersFactory
import com.google.android.exoplayer2.offline.*
import com.google.android.exoplayer2.scheduler.Requirements
import com.google.android.exoplayer2.trackselection.MappingTrackSelector.MappedTrackInfo
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.util.Log
import com.google.android.exoplayer2.util.Util
import java.io.IOException
import java.util.*
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Tracks media that has been downloaded.
 */
open class DownloadTracker(
    context: Context, dataSourceFactory: DataSource.Factory, downloadManager: DownloadManager
) {
    /**
     * Listens for changes in the tracked downloads.
     */
    interface Listener {
        /**
         * Called when the tracked downloads changed.
         */
        fun onDownloadsChanged()
    }

    @Suppress("JoinDeclarationAndAssignment")
    private val context: Context
    private val dataSourceFactory: DataSource.Factory
    private val listeners: CopyOnWriteArraySet<Listener>
    private val downloads: HashMap<Uri, Download?>
    private val downloadIndex: DownloadIndex
    private var startDownloadDialogHelper: StartDownloadDialogHelper? = null

    fun addListener(listener: Listener) {
        listeners.add(listener)
    }

    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    fun isDownloaded(uri: Uri): Boolean {
        val download = downloads[uri]
        return download != null && download.state != Download.STATE_FAILED
    }

    fun getDownloadRequest(uri: Uri): DownloadRequest? {
        val download = downloads[uri]
        return if (download != null && download.state != Download.STATE_FAILED) download.request else null
    }

    fun toggleDownload(
        fragmentManager: FragmentManager?,
        name: String?,
        uri: Uri,
        extension: String,
        renderersFactory: RenderersFactory
    ) {
        val download = downloads[uri]
        if (startDownloadDialogHelper != null) {
            startDownloadDialogHelper!!.release()
        }
        startDownloadDialogHelper = StartDownloadDialogHelper(
            fragmentManager, getDownloadHelper(uri, extension, renderersFactory), name
        )
    }

    fun addDownload(
        fragmentManager: FragmentManager?,
        name: String?,
        uri: Uri,
        extension: String,
        renderersFactory: RenderersFactory
    ) {
        val download = downloads[uri]
        if (startDownloadDialogHelper != null) {
            startDownloadDialogHelper!!.release()
        }
        startDownloadDialogHelper = StartDownloadDialogHelper(
            fragmentManager, getDownloadHelper(uri, extension, renderersFactory), name
        )
    }

    fun removeDownload(uri: Uri) {
        val download = downloads[uri]
        DownloadService.sendRemoveDownload(
            context, DemoDownloadService::class.java, download!!.request.id,  /* foreground= */false
        )
    }

    fun loadDownloads() {
        try {
            downloadIndex.getDownloads().use { loadedDownloads ->
                while (loadedDownloads.moveToNext()) {
                    val download = loadedDownloads.download
                    downloads[download.request.uri] = download
                }
            }
        } catch (e: IOException) {
            Log.w(TAG, "Failed to query downloads", e)
        }
    }

    fun loadDownloaded(): List<Download> {
        val downloaded: List<Download> = ArrayList()
        try {
            downloadIndex.getDownloads().use { loadedDownloads ->
                while (loadedDownloads.moveToNext()) {
                    val download = loadedDownloads.download
                    downloads[download.request.uri] = download
                }
            }
        } catch (e: IOException) {
            Log.w(TAG, "Failed to query downloads", e)
        }
        return downloaded
    }

    fun loadDownloadsUri(): ArrayList<Uri> {
        val localdownloads = ArrayList<Uri>()
        var download: Download? = null
        try {
            downloadIndex.getDownloads().use { loadedDownloads ->
                while (loadedDownloads.moveToNext()) {
                    download = loadedDownloads.download
                    downloads[download!!.request.uri] = download
                }
                for (i in 0 until downloads.size) {
                    localdownloads.add(download!!.request.uri)
                }
            }
        } catch (e: IOException) {
            Log.w(TAG, "Failed to query downloads", e)
        }
        return localdownloads
    }

    private fun getDownloadHelper(
        uri: Uri, extension: String, renderersFactory: RenderersFactory
    ): DownloadHelper {
        return when (val type = Util.inferContentType(uri, extension)) {
            C.TYPE_DASH -> DownloadHelper.forDash(uri, dataSourceFactory, renderersFactory)
            C.TYPE_SS -> DownloadHelper.forSmoothStreaming(uri, dataSourceFactory, renderersFactory)
            C.TYPE_HLS -> DownloadHelper.forHls(uri, dataSourceFactory, renderersFactory)
            C.TYPE_OTHER -> DownloadHelper.forProgressive(uri)
            else -> throw IllegalStateException("Unsupported type: $type")
        }
    }

    inner class DownloadManagerListener : DownloadManager.Listener {
        override fun onDownloadChanged(downloadManager: DownloadManager, download: Download) {
            downloads[download.request.uri] = download
            for (listener in listeners) {
                listener.onDownloadsChanged()
            }
        }

        override fun onDownloadRemoved(downloadManager: DownloadManager, download: Download) {
            downloads.remove(download.request.uri)
            for (listener in listeners) {
                listener.onDownloadsChanged()
            }
        }
    }

    private inner class StartDownloadDialogHelper(
        private val fragmentManager: FragmentManager?,
        private val downloadHelper: DownloadHelper,
        private val name: String?
    ) :
        DownloadHelper.Callback, DialogInterface.OnClickListener,
        DialogInterface.OnDismissListener {
        private var mappedTrackInfo: MappedTrackInfo? = null
        fun release() {
            downloadHelper.release()
        }

        // DownloadHelper.Callback implementation.
        override fun onPrepared(helper: DownloadHelper) {
            if (helper.periodCount == 0) {
                Log.d(TAG, "No periods found. Downloading entire stream.")
                startDownload()
                downloadHelper.release()
                return
            }
            mappedTrackInfo = downloadHelper.getMappedTrackInfo( /* periodIndex= */0)
            Log.d(TAG, "No dialog content. Downloading entire stream.")
            startDownload()
            downloadHelper.release()
            return
        }

        override fun onPrepareError(helper: DownloadHelper, e: IOException) {
            Toast.makeText(
                context.applicationContext, "R.string.download_start_error", Toast.LENGTH_LONG
            )
                .show()
            Log.e(TAG, "Failed to start download", e)
        }

        // DialogInterface.OnClickListener implementation.
        override fun onClick(dialog: DialogInterface, which: Int) {
            for (periodIndex in 0 until downloadHelper.periodCount) {
                downloadHelper.clearTrackSelections(periodIndex)
                for (i in 0 until mappedTrackInfo!!.rendererCount) {
                    downloadHelper.addTrackSelectionForSingleRenderer(
                        periodIndex,
                        i,
                        DownloadHelper.DEFAULT_TRACK_SELECTOR_PARAMETERS,
                        null
                    )
                }
            }
            val downloadRequest = buildDownloadRequest()
            if (downloadRequest.streamKeys.isEmpty()) {
                // All tracks were deselected in the dialog. Don't start the download.
                return
            }
            startDownload(downloadRequest)
        }

        // DialogInterface.OnDismissListener implementation.
        override fun onDismiss(dialogInterface: DialogInterface) {
            downloadHelper.release()
        }

        // Internal methods.
        private fun startDownload(downloadRequest: DownloadRequest = buildDownloadRequest()) {
            Log.d(TAG, "startdownload")
            Log.d(TAG, downloadRequest.toString())
            try {
                DownloadService.sendAddDownload(
                    context,
                    DemoDownloadService::class.java,
                    downloadRequest,  /* foreground= */
                    true
                )
            } catch (e: Exception) {
                Toast.makeText(context, "Please Reconnect Your Network!!!", Toast.LENGTH_SHORT)
                    .show()
                e.printStackTrace()
            }
        }

        private fun buildDownloadRequest(): DownloadRequest {
            return downloadHelper.getDownloadRequest(
                Util.getUtf8Bytes(
                    name
                )
            )
        }

        init {
            downloadHelper.prepare(this)
        }
    }

    companion object {
        private const val TAG = "DownloadTracker"
        val requirements: Requirements
            get() = Requirements(Requirements.NETWORK)
    }

    init {
        this.context = context.applicationContext
        this.dataSourceFactory = dataSourceFactory
        listeners = CopyOnWriteArraySet()
        downloads = HashMap()
        downloadIndex = downloadManager.downloadIndex
        downloadManager.addListener(DownloadManagerListener())
        loadDownloads()
    }
}
