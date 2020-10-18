@file:Suppress("PackageName")

package com.afsar.customflix.Modal

data class DownloadModal(
    var id: String,
    var name: String,
    var image: String,
    var link: String,
    var duration: String,
    var progress: Float,
    var inprogress: Boolean
)
