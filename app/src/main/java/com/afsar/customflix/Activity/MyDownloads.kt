@file:Suppress("HasPlatformType", "PrivatePropertyName", "PackageName")

package com.afsar.customflix.Activity

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afsar.customflix.Modal.DownloadModal
import com.afsar.customflix.Modal.Downloads
import com.afsar.customflix.Player.PlayerActivity
import com.afsar.customflix.R
import com.afsar.customflix.Utilities.CFApplication
import com.afsar.customflix.Utilities.DBHelper
import com.afsar.customflix.Utilities.DownloadTracker
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.offline.Download
import com.google.android.exoplayer2.offline.DownloadManager
import kotlinx.android.synthetic.main.activity_my_downloads.*
import kotlinx.android.synthetic.main.download_item.view.*
import java.util.*
import kotlin.collections.ArrayList

class MyDownloads : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var sAdapter: CustomAdapter
    private var list: List<Downloads> = ArrayList()
    private var dList: ArrayList<DownloadModal> = ArrayList()
    private lateinit var dbHelper: DBHelper
    private lateinit var downloadTracker: DownloadTracker
    private lateinit var downloadManager: DownloadManager
    private val TAG: String = "MyDownloadPage"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_downloads)
        recyclerView = findViewById(R.id.downloads_recycler)

        val application: CFApplication =
            Objects.requireNonNull(
                applicationContext as CFApplication
            )
        downloadTracker = application.getDownloadTracker()
        downloadManager = application.getDownloadManager()!!
        dbHelper = DBHelper(applicationContext)
        downloadManager.addListener(object : DownloadManager.Listener {
            override fun onDownloadChanged(downloadManager: DownloadManager, download: Download) {
                Log.d(TAG, "onDownloadChanged:Here")
            }
        })
        recyclerView.apply {
            val linearLayoutManager =
                LinearLayoutManager(applicationContext, LinearLayoutManager.VERTICAL, false)
            recyclerView.layoutManager = linearLayoutManager
            sAdapter = CustomAdapter(dList, applicationContext)
            recyclerView.adapter = sAdapter
            sAdapter.notifyDataSetChanged()
        }
        loadList()
        downloadback.setOnClickListener {
            finish()
        }
    }

    private fun loadList() {
        dList.clear()
        list = dbHelper.allDownloads
        try {
            when {
                list.isNotEmpty() -> {
                    for (i in list.indices) {
                        Log.d(TAG, "loadList:${list[i].name}")
                        val downloadModal = DownloadModal(
                            list[i].id.toString(),
                            list[i].name!!,
                            list[i].image!!,
                            list[i].path!!,
                            list[i].duration!!,
                            getProgress(list[i].path!!),
                            false
                        )
                        dList.add(downloadModal)
                        sAdapter.notifyDataSetChanged()
                    }
                }
            }
            Log.d(TAG, "loadList:${list.size}")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getProgress(string: String): Float {
        Log.d(TAG, "getProgress:$string")
        var download = 0.0f
        for (i in downloadManager.currentDownloads.indices) {
            Log.d(
                "getPercentDownloaded",
                "{{ " + downloadManager.currentDownloads[0].percentDownloaded
            )
            download = downloadManager.currentDownloads[0].percentDownloaded
        }
        return download
    }

    inner class CustomAdapter(private val slist: ArrayList<DownloadModal>, val context: Context) :
        RecyclerView.Adapter<CustomAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.download_item, parent, false)
            return ViewHolder(v)
        }

        override fun getItemCount(): Int {
            return slist.size
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            Glide.with(context)
                .asBitmap()
                .load(slist[position].image)
                .error(R.drawable.customflix)
                .into(holder.img)
            holder.name.text = slist[position].name
            holder.removedownload.setOnClickListener {
                removeDownloads(Uri.parse(slist[position].link), slist[position].link)
            }
            holder.itemView.download_body.setOnClickListener {
                val intent = Intent(context, PlayerActivity::class.java)
                intent.putExtra("title", slist[position].name)
                intent.putExtra("desc", slist[position].name)
                intent.putExtra("pic", slist[position].image)
                intent.putExtra("url", slist[position].link)
                PlayerActivity.id = slist[position].id
                PlayerActivity.eng_banner = slist[position].image
                PlayerActivity.vtitle =slist[position].name
                PlayerActivity.desc = slist[position].name
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            }
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val name = itemView.findViewById<TextView>(R.id.head_title)
            val img = itemView.findViewById<ImageView>(R.id.circular_image)
            val removedownload = itemView.findViewById<TextView>(R.id.head_title2)
        }
    }

    private fun removeDownloads(uri: Uri, url: String) {
        try {
            val application: CFApplication =
                Objects.requireNonNull(
                    applicationContext as CFApplication
                )
            val downloadTracker: DownloadTracker = application.getDownloadTracker()
            downloadManager = application.getDownloadManager()!!
            when {
                dbHelper.checkDownloads(url) -> {
                    dbHelper.deleteDownloadsByPath(url)
                }
            }
            try {
                downloadTracker.removeDownload(uri)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            sAdapter.notifyDataSetChanged()
            finish()
            overridePendingTransition(0, 0)
            startActivity(intent)
            overridePendingTransition(0, 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        Log.d("CustomFlix", "called")
        finish()
    }
}