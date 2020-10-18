@file:Suppress("PackageName")

package com.afsar.customflix.Modal

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class Videos(
    @SerializedName("vthumb")
    @Expose
    var image: String,
    @SerializedName("vid_id")
    @Expose
    var id: String,
    @SerializedName("vdesc")
    @Expose
    var desc: String,
    @SerializedName("url")
    @Expose
    var link: String,
    @SerializedName("video_name")
    @Expose
    var name: String
)