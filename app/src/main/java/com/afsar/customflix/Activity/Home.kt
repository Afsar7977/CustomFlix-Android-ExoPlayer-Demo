@file:Suppress(
    "DEPRECATION", "PackageName",
    "UNUSED_VARIABLE", "PrivatePropertyName"
)

package com.afsar.customflix.Activity

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afsar.customflix.Modal.Videos
import com.afsar.customflix.Network.Status
import com.afsar.customflix.Player.PlayerActivity
import com.afsar.customflix.R
import com.afsar.customflix.Service.NetworkListener
import com.afsar.customflix.VModel.VModel
import com.bumptech.glide.Glide
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.videos_item.view.*
import kotlin.properties.Delegates

class Home : AppCompatActivity() {

    private lateinit var vModel: VModel
    private val TAG = "Home"
    private var vList: ArrayList<Videos> = ArrayList()
    private lateinit var sAdapter: CustomAdapter
    private var vPosition by Delegates.notNull<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        vModel = ViewModelProviders.of(this).get(VModel::class.java)
        loadVideos()

        video_recycler.apply {
            val linearLayoutManager =
                LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            video_recycler.layoutManager = linearLayoutManager
            sAdapter = CustomAdapter(vList, applicationContext)
            video_recycler.adapter = sAdapter
            sAdapter.notifyDataSetChanged()
        }

        mydownloads.setOnClickListener {
            val intent = Intent(applicationContext, MyDownloads::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }

        playVideo.setOnClickListener {
            loadLog("playVideo", vList[vPosition].name)
            val intent = Intent(applicationContext, PlayerActivity::class.java)
            intent.putExtra("title", vList[vPosition].name)
            intent.putExtra("image", vList[vPosition].image)
            intent.putExtra("desc", vList[vPosition].desc)
            intent.putExtra("url", vList[vPosition].link)
            PlayerActivity.id = vList[vPosition].id
            PlayerActivity.eng_banner = vList[vPosition].image
            PlayerActivity.vtitle = vList[vPosition].name
            PlayerActivity.desc = vList[vPosition].desc
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
    }

    inner class CustomAdapter(private val subList: ArrayList<Videos>, val context: Context) :
        RecyclerView.Adapter<CustomAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.videos_item, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.itemView.name.text = subList[position].name
            Picasso.get().load(subList[position].image).into(holder.itemView.img)
            holder.itemView.video_body.setOnClickListener {
                updateForeground(position, subList[position].image)
            }
            holder.bindItems()
        }

        override fun getItemCount(): Int {
            return subList.size
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun bindItems() {
                val name = itemView.findViewById<TextView>(R.id.name)
                val img = itemView.findViewById<ImageView>(R.id.img)
            }
        }
    }

    private fun loadVideos() {
        vModel.getVideos("2").observe(this, {
            it?.let { resource ->
                when (resource.status) {
                    Status.LOADING -> {
                        loadLog("Loading", "${it.status}")
                    }
                    Status.SUCCESS -> {
                        loadLog("Success", "${it.data?.body()}")
                        vList.clear()
                        vList.addAll(it.data?.body()!!)
                        sAdapter.notifyDataSetChanged()
                    }
                    Status.ERROR -> {
                        loadLog("Error", "${it.message}")
                    }
                }
            }
        })
    }

    private fun loadLog(key: String, data: String) {
        Log.d(TAG, "loadLog:$key::$data")
    }

    private fun updateForeground(position: Int, imageString: String) {
        try {
            vPosition = position
            playVideo.visibility = View.VISIBLE
            Glide.with(applicationContext)
                .load(imageString)
                .error(R.drawable.customflix)
                .into(foreground)
        } catch (e: Exception) {
            loadLog("updateForeground", e.toString())
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            NetworkListener.unregister(this, broadcastReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val broadcastReceiver by lazy {
        NetworkListener.create({
            Log.d("broadcastReceiver", ":NetworkUp()")
        }, {
            val i = Intent(this, MyDownloads::class.java)
            startActivity(i)
            finish()
        })
    }

    override fun onResume() {
        super.onResume()
        NetworkListener.register(this, broadcastReceiver)
    }
}