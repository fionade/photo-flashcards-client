package de.lmu.personalflashcards.uielements

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.google.android.material.snackbar.Snackbar
import de.lmu.personalflashcards.Logger
import de.lmu.personalflashcards.MainActivity
import de.lmu.personalflashcards.R
import de.lmu.personalflashcards.objectdetection.GoogleRequestDetection
import de.lmu.personalflashcards.objectdetection.MyVolleyRequests
import kotlinx.android.synthetic.main.fragment_onboarding.view.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import org.json.JSONException
import org.json.JSONObject


class OnboardingFragment: Fragment() {

    companion object {
        const val TAG = "Onboarding"
    }

    // Logging
    private lateinit var logger: Logger

    private var isInitial = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_onboarding, container, false)

        // Logging
        logger = Logger(requireContext())

        val cm = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = cm.activeNetworkInfo
        val isConnected: Boolean = activeNetwork?.isConnectedOrConnecting == true

        if (!isConnected) {
            Snackbar.make(requireActivity().findViewById(android.R.id.content), R.string.no_connection, Snackbar.LENGTH_SHORT).show()
        }

        arguments?.getBoolean("isInitial")?.let {
            isInitial = it
        }

        logger.addLogMessage("started_survey", "is pre-study survey: $isInitial")

        val sharedPreferences = activity?.getSharedPreferences(
            getString(R.string.preference_file_key), Context.MODE_PRIVATE)
        val userKey = sharedPreferences?.getString("user_key", "")

        val jsonObject = JSONObject()
        jsonObject.put("user_key", userKey)

        val url = if (isInitial) MainActivity.BASE_URL.format("getToken") else MainActivity.BASE_URL.format("getPostToken")

        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonObject,
            Response.Listener { response ->

                try {
                    val token = response.get("token")
                    logger.addLogMessage("token_received", token.toString())
                    root.preSurveyWebView.settings.javaScriptEnabled = true
                    val surveyUrl = if (isInitial)
                        ".../token/${token}/" else
                        ".../token/${token}/"
                    root.preSurveyWebView.loadUrl(surveyUrl)

                    root.preSurveyWebView.webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            Uri.parse(url).host?.let {
                                if (it.contains("example.com", true)) {
                                    onComplete()
                                    return false
                                }
                            }

                            // Otherwise, the link is not for a page of ours, so do nothing
                            return true
                        }
                    }

                }
                catch (e: JSONException) {
                    Log.d(GoogleRequestDetection.TAG, "could not parse JSON")
                }

                Log.d(TAG, "Successfully retrieved token")

            }, Response.ErrorListener {
                // not much we can do here. We'll just try again later
                Log.d(TAG, "could not get token")
            })

        val volleyRequests = MyVolleyRequests.getInstance(requireActivity())
        volleyRequests.requestQueue.add(request)

        return root
    }

    fun onComplete() {
        activity?.let {
            if (isInitial) {
                it.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE).edit()
                    .putBoolean("surveyDone", true)
                    .putLong("startTime", System.currentTimeMillis()) // also store when the survey  was done
                    .apply()
            }
            else {
                it.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE).edit()
                    .putBoolean("postSurveyDone", true)
                    .apply()
            }
            logger.addLogMessage("finished_survey", "is pre-study survey: $isInitial")
            findNavController().navigateUp()
        }

    }

}