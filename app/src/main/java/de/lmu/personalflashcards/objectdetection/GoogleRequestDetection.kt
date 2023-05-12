package de.lmu.personalflashcards.objectdetection

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import de.lmu.personalflashcards.MainActivity
import de.lmu.personalflashcards.R
import de.lmu.personalflashcards.model.Quiz
import de.lmu.personalflashcards.model.SerializableQuiz
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.json.JSONException
import org.json.JSONObject


class GoogleRequestDetection constructor(private val context: Context) {

    companion object {
        const val TAG = "GoogleRequest"
    }

    fun runObjectDetection(image: Bitmap, imagePath: String, thumbnailPath: String, captureTime: String?, successCallback: (Quiz) -> Unit, errorCallback: (VolleyError) -> Unit) {

        val url = MainActivity.BASE_URL.format("analyseImage")
//        val url = MainActivity.BASE_URL.format("testQuiz")

        val volleyRequests = MyVolleyRequests.getInstance(context)

        val jsonObject = JSONObject()
        jsonObject.put("image",  (volleyRequests.getStringImage(image)))
        jsonObject.put("width", image.width)
        jsonObject.put("height", image.height)
        jsonObject.put("captureTime", captureTime)

        val sharedPreferences = context.getSharedPreferences(
            context.getString(R.string.preference_file_key), Context.MODE_PRIVATE)
        val userKey = sharedPreferences.getString("user_key", "")
        jsonObject.put("user_key", userKey)

        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonObject,
            Response.Listener { response ->
                Log.d(TAG, response.toString())

                // create Quiz Object
                try {
                    val serializableQuiz = Json.decodeFromString<SerializableQuiz>(response.getJSONObject("quiz").toString())
                    serializableQuiz.imagePath = imagePath
                    serializableQuiz.thumbnailPath = thumbnailPath
                    val quiz = serializableQuiz.toQuiz()
                    successCallback(quiz)
                    //                val boundingBox1 =
                }
                catch (e: JSONException) {
                    Log.d(TAG, "could not parse JSON")
                    errorCallback(VolleyError(e.message))
                }

            }, Response.ErrorListener { error ->
                errorCallback(error)
            })

        request.retryPolicy = DefaultRetryPolicy(0, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)

        volleyRequests.requestQueue.add(request)
    }
}