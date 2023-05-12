package de.lmu.personalflashcards.objectdetection

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import java.io.ByteArrayOutputStream

class MyVolleyRequests constructor(context: Context) {
    companion object {
        @Volatile
        private var INSTANCE: MyVolleyRequests? = null
        fun getInstance(context: Context) =
            INSTANCE
                ?: synchronized(this) {
                INSTANCE
                    ?: MyVolleyRequests(
                        context
                    ).also {
                    INSTANCE = it
                }
            }
    }
    val requestQueue: RequestQueue by lazy {
        // applicationContext is key, it keeps you from leaking the
        // Activity or BroadcastReceiver if someone passes one in.
        Volley.newRequestQueue(context.applicationContext)
    }
    fun <T> addToRequestQueue(req: Request<T>) {
        requestQueue.add(req)
    }

    fun getStringImage(bmp: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 80, baos)
        val imageBytes: ByteArray = baos.toByteArray()
        return Base64.encodeToString(imageBytes, Base64.DEFAULT)
    }
}