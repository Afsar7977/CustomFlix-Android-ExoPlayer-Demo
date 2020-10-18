@file:Suppress("PackageName", "unused")

package com.afsar.customflix.Network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

open class Utils {
    companion object {
        var retrofit1: Retrofit = Retrofit.Builder()
            .baseUrl("https://www.json-generator.com/api/json/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        var TAG = "Utils"
    }
}