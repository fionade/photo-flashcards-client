package de.lmu.personalflashcards

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.Response.Listener
import com.android.volley.toolbox.JsonObjectRequest
import de.lmu.personalflashcards.datahandling.LogViewModel
import de.lmu.personalflashcards.model.LogEntry
import de.lmu.personalflashcards.objectdetection.MyVolleyRequests
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Executors

class Logger(context: Context) {

    companion object {
        private const val TAG = "Logger"
    }

    private val logViewModel: LogViewModel = ViewModelProvider(context as ViewModelStoreOwner).get(LogViewModel::class.java)
    private val volleyRequests = MyVolleyRequests.getInstance(context)
    private var userKey: String?

    init {
        val sharedPreferences = context.getSharedPreferences(
            context.getString(R.string.preference_file_key), Context.MODE_PRIVATE)
        userKey = sharedPreferences.getString("user_key", "")
    }

    fun addLogMessage(type: String, values: String) {
        val timestamp = System.currentTimeMillis()
        val logEntry = LogEntry(timestamp, type, values)
        logViewModel.insert(logEntry)
    }

    fun addQuizLogMessage(type: String, values: String, quizId: Long) {
        val timestamp = System.currentTimeMillis()
        val logEntry = LogEntry(timestamp, type, values, quizId)
        logViewModel.insert(logEntry)
    }

}