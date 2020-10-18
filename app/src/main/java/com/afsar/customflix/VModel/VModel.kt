package com.afsar.customflix.VModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.afsar.customflix.Network.Resource
import com.afsar.customflix.Network.Utils
import com.afsar.customflix.Network.WebApi
import kotlinx.coroutines.Dispatchers

class VModel : ViewModel() {

    private val service1 = Utils.retrofit1.create(WebApi::class.java)

    fun getVideos(string: String) = liveData(Dispatchers.IO) {
        indentId = string
        emit(Resource.loading(data = null))
        try {
            emit(Resource.success(data = getVideosData()))
        } catch (exception: Exception) {
            emit(Resource.error(data = null, message = exception.message ?: "Error Occurred!"))
        }
    }

    suspend fun getVideosData() = service1.getVideos(
        indentId
    )

    companion object {
        private lateinit var indentId: String
    }
}