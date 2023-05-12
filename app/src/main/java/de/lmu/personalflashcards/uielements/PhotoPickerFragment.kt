package de.lmu.personalflashcards.uielements

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Bundle
import android.os.Environment
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.google.android.material.snackbar.Snackbar
import de.lmu.personalflashcards.Logger
import de.lmu.personalflashcards.MainActivity
import de.lmu.personalflashcards.R
import de.lmu.personalflashcards.datahandling.QuizViewModel
import de.lmu.personalflashcards.model.SerializableQuiz
import de.lmu.personalflashcards.objectdetection.GoogleRequestDetection
import de.lmu.personalflashcards.objectdetection.MyVolleyRequests
import kotlinx.android.synthetic.main.fragment_photo_picker.view.*
import kotlinx.android.synthetic.main.fragment_quiz.*
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.apache.commons.io.IOUtils
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class PhotoPickerFragment: Fragment(), PhotoListAdapter.OnCardClickListener {

    companion object {
        const val TAG = "PhotoPicker"
    }

    // Logging
    private lateinit var logger: Logger

    private lateinit var quizViewModel: QuizViewModel

    private lateinit var photoPickerListView: RecyclerView
    private lateinit var progressView: View

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_photo_picker, container, false)

        // Logging
        logger = Logger(requireContext())
        logger.addLogMessage("photo_picker", "started")

        quizViewModel = ViewModelProvider(this).get(QuizViewModel::class.java)

        val photoListViewManager = GridLayoutManager(requireActivity(), 3)
        val photoListAdapter = PhotoListAdapter(this, requireContext())

        photoPickerListView = root.photoPickerListView
        root.photoPickerListView.apply {
            setHasFixedSize(true)
            layoutManager = photoListViewManager
            adapter = photoListAdapter
        }

        progressView = root.predefinedLoadProgress

        context?.let {
            val images = it.assets.list("static_images")

            images?.map {
                "static_images/${it}"
            }?.let { it2 ->
                photoListAdapter.setPhotos(it2)
            }

        }

        return root
    }

    override fun onCardClicked(imagePath: String) {

        // get file and quiz for the selected image
        val cm = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = cm.activeNetworkInfo
        val isConnected: Boolean = activeNetwork?.isConnectedOrConnecting == true

        if (isConnected) {
            context?.let {
                progressView.visibility = View.VISIBLE
                photoPickerListView.visibility = View.GONE

                val url = MainActivity.BASE_URL.format("getPredefinedImage")

                val volleyRequests = MyVolleyRequests.getInstance(it)

                val imageName = imagePath.replace("static_images/", "")
                val jsonObject = JSONObject()
                jsonObject.put("imagePath", imageName)

                val sharedPreferences = it.getSharedPreferences(
                    it.getString(R.string.preference_file_key), Context.MODE_PRIVATE)
                val userKey = sharedPreferences.getString("user_key", "")
                jsonObject.put("user_key", userKey)

                val request = JsonObjectRequest(
                    Request.Method.POST, url, jsonObject,
                    Response.Listener { response ->

                        try {
                            // store the image received from the server
                            saveBase64Image(response.get("image").toString(), response.get("imageName").toString())?.let { it2 ->

                                val serializableQuiz = Json.decodeFromString<SerializableQuiz>(response.getJSONObject("quiz").toString())
                                serializableQuiz.imagePath = it2
                                val thumbnailPath = copyThumbnailFile(imagePath, imageName)
                                serializableQuiz.thumbnailPath = if (!thumbnailPath.isNullOrEmpty()) thumbnailPath else ""
                                val quiz = serializableQuiz.toQuiz()

                                logger.addLogMessage("added_predefined", imageName)

                                // insert into database
                                quizViewModel.insert(quiz)
                                quizViewModel.getQuizByImage(quiz.imagePath)
                                quizViewModel.quizByImage.observe(viewLifecycleOwner, Observer { quizzes ->
                                    if (quizzes.isNotEmpty()) {

                                        val insertedQuiz = quizzes[0]

                                        // open the photo quiz fragment to show a detail view of the newly created quiz
                                        val bundle = bundleOf("openPicker" to false, "showQuiz" to insertedQuiz.id)
                                        lifecycleScope.launch {
                                            logger.addQuizLogMessage("show_quiz", "new", insertedQuiz.id)
                                        }

                                        // we use this action to avoid popping back to the new quiz view
                                        findNavController().navigate(R.id.action_new_to_photo_quiz, bundle)
                                    }
                                })

                            }

                        }
                        catch (e: JSONException) {
                            Log.d(GoogleRequestDetection.TAG, "could not parse JSON")
                            // error handling
                            view?.let { view ->
                                Snackbar.make(view, R.string.image_error, Snackbar.LENGTH_SHORT).show()
                            }
                            findNavController().navigateUp()
                        }

                    }, Response.ErrorListener { _ ->
                        logger.addLogMessage("quiz_new_error", "couldn't create")
                        view?.let { it2 ->
                            Snackbar.make(it2, R.string.image_error, Snackbar.LENGTH_SHORT).show()
                            findNavController().navigateUp()
                        }
                    })

                volleyRequests.requestQueue.add(request)
            }
        }
        else {
            view?.let {
                Snackbar.make(it, R.string.no_connection, Snackbar.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        }
    }


    private fun saveBase64Image(imageData: String, fileName: String): String? {

        context?.let {
            val directory = File(it.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "flashcard_pictures")
            directory.mkdirs()

            val file = File(directory, fileName)

            try {
                val decodedString: ByteArray = Base64.decode(imageData, Base64.DEFAULT)
                file.writeBytes(decodedString)
            } catch (e: Exception) {
                e.message?.let { it1 -> Log.e(TAG, it1) }
            }

            return file.path
        }

        return null

    }

    private fun copyThumbnailFile(imagePath: String, imageName: String ): String? {

        context?.let {
            val directory =
                File(it.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "flashcard_pictures")
            directory.mkdirs()

            val file = File(directory, imageName.replace(".", "_thumbnail."))

            it.assets.open(imagePath).use { it2 ->
                val outputStream = FileOutputStream(file)
                IOUtils.copy(it2, outputStream)
            }

            return file.path
        }

        return null

    }

}