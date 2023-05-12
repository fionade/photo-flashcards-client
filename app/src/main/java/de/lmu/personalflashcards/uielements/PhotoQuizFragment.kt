package de.lmu.personalflashcards.uielements

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.media.ThumbnailUtils
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import de.lmu.personalflashcards.Logger
import de.lmu.personalflashcards.MainActivity
import de.lmu.personalflashcards.R
import de.lmu.personalflashcards.datahandling.QuizViewModel
import de.lmu.personalflashcards.model.Quiz
import de.lmu.personalflashcards.objectdetection.GoogleRequestDetection
//import de.lmu.personalflashcards.objectdetection.LocalCategoryDetection
import kotlinx.android.synthetic.main.fragment_quiz.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

class PhotoQuizFragment: Fragment() {

    companion object {
        const val ODT_REQUEST_LIBRARY_PHOTO = 15
        const val MAX_IMAGE_DIMENSION = 1920
        const val BITMAP_DIMENSION = 300

        const val TAG = "PhotoQuizFragment"
    }

    // Logging
    private lateinit var logger: Logger

    // Google Vision
    private lateinit var googleRequestDetection: GoogleRequestDetection

    private var quizId: Long = -1
    private lateinit var quiz: Quiz
    private lateinit var quizViewModel: QuizViewModel

    private lateinit var rootView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val root = inflater.inflate(R.layout.fragment_quiz, container, false)


        // Logging
        logger = Logger(requireContext())

        googleRequestDetection =
            GoogleRequestDetection(
                requireActivity().applicationContext
            )

        quizViewModel = ViewModelProvider(this).get(QuizViewModel::class.java)

        arguments?.getBoolean("openPicker")?.let { it ->
            if (it) {
                logger.addLogMessage("opened_quiz_view", "new quiz")
                onNewPhotoQuizClicked()
            }
            else {
                arguments?.getLong("showQuiz")?.let { it2 ->
                    quizId = it2
                    if (it2 > -1) {
                        quizViewModel.getQuiz(it2)
                        quizViewModel.oneQuiz.observe(viewLifecycleOwner, Observer { quizzes ->
                            if (quizzes.isNotEmpty()) {
                                quiz = quizzes[0]
                                if (quiz.predefined) {
                                    try {
                                        requireContext().assets.open(quiz.imagePath).use {
                                            imageView.setImageDrawable(Drawable.createFromStream(it, null))
                                        }
                                    }
                                    catch (e: IOException) {
                                        Log.d(TAG, "could not find image")
                                    }
                                }
                                else {
                                    val pictureFile = File(quiz.imagePath)
                                    val bitmap = BitmapFactory.decodeFile(pictureFile.absolutePath)
                                    imageView.setImageBitmap(bitmap)
                                }

                                setQuiz()

                                logger.addQuizLogMessage("opened_quiz_view", "", quiz.id)
                            }
                        })
                    }
                }
            }
        }

        rootView = root
        return root
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ODT_REQUEST_LIBRARY_PHOTO) {

            if (resultCode == AppCompatActivity.RESULT_OK
            ) {
                val imageUri = data!!.data

                // saving the file to external app-specific storage
                if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {

                    // Copy file to local directory
                    val directory = File(context?.getExternalFilesDir(
                        Environment.DIRECTORY_PICTURES), "flashcard_pictures")
                    directory.mkdirs()

                    val uuid = UUID.randomUUID()
                    val file = File(directory, "${uuid}.jpg")
                    Log.d(TAG, "imagePath: ${file.path}")
                    val thumbnailFile = File(directory, "${uuid}_thumbnail.jpg")

                    val parcelFileDescriptor = requireContext().contentResolver.openFileDescriptor(imageUri!!, "r", null)

                    parcelFileDescriptor?.let {
                        val inputStream = FileInputStream(parcelFileDescriptor.fileDescriptor)
                        val outputStream = FileOutputStream(file)
                        IOUtils.copy(inputStream, outputStream)
                    }

                    val image = MediaStore.Images.Media.getBitmap(activity?.contentResolver, imageUri)
                    //            Picasso.get().load(imageUri).into(imageView)
                    quizLoadProgress.visibility = View.VISIBLE

                    val orientation = getImageOrientation(file.path)
                    val rescaledImage = scaleAndRotateImage(image, orientation, MAX_IMAGE_DIMENSION)

                    imageView.setImageBitmap(rescaledImage)

                    lifecycleScope.launch(Dispatchers.IO) {
                        // create a thumbnail image

                        // save the image
                        try {
                            FileOutputStream(file).use { out ->
                                rescaledImage?.compress(
                                    Bitmap.CompressFormat.JPEG,
                                    80,
                                    out
                                )
                            }

                            // also save thumbnail image
                            val thumbnail = ThumbnailUtils.extractThumbnail(rescaledImage, BITMAP_DIMENSION, BITMAP_DIMENSION)
                            FileOutputStream(thumbnailFile).use { out ->
                                thumbnail.compress(
                                    Bitmap.CompressFormat.JPEG,
                                    80,
                                    out
                                )
                            }

                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }

                    // check internet connection
                    // TODO replace deprecated calls
                    val cm = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                    val activeNetwork: NetworkInfo? = cm.activeNetworkInfo
                    val isConnected: Boolean = activeNetwork?.isConnectedOrConnecting == true

                    if (isConnected) {
                        rescaledImage?.let {
                            val captureTime = getCaptureTime(file.path)

                            googleRequestDetection.runObjectDetection(rescaledImage, file.path, thumbnailFile.path, captureTime, { quiz ->

                                quizViewModel.insert(quiz)
                                this.quiz = quiz

                                lifecycleScope.launch {
                                    logger.addQuizLogMessage("added_quiz", quiz.german, quiz.id)
                                }

                                activity?.runOnUiThread {
                                    setQuiz()
                                }
                            }, { error ->
                                Log.e(MainActivity.TAG, error.toString())
                                Snackbar.make(rootView, R.string.image_error, Snackbar.LENGTH_SHORT).show()
                                quizLoadProgress.visibility = View.GONE

                                findNavController().navigateUp()

                                // delete the temporarily created files
                                thumbnailFile.delete()
                                file.delete()
                            } )
                        }
                    }
                    else {
                        Snackbar.make(rootView, R.string.no_connection, Snackbar.LENGTH_SHORT).show()
                        quizLoadProgress.visibility = View.GONE
                        findNavController().navigateUp()
                        // TODO delete files again?
                    }

                }

                else {
                    Log.d(TAG, "Issue accessing file storage")
                }
            }
            else {
                findNavController().navigateUp()
            }

        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (quizId > -1) {
            inflater.inflate(R.menu.card_context_menu, menu)
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.delete_item_action -> {
                deleteQuiz()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun deleteQuiz() {

        val builder: AlertDialog.Builder? = activity?.let {
            AlertDialog.Builder(it)
        }

        builder?.apply {
            setMessage(getString(R.string.delete_confirmation))
            setTitle(R.string.delete_card_title)
            setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }
            setPositiveButton(R.string.delete
            ) { _, _ ->
                quizViewModel.deleteQuiz(quiz)
                val file = File(quiz.imagePath)
                val thumbnailFile = File(quiz.thumbnailPath)
                file.delete()
                thumbnailFile.delete()
                findNavController().navigateUp()
                logger.addQuizLogMessage("quiz_deleted", "", quiz.id)
            }
        }

        builder?.create()?.show()
    }

    private fun setQuiz() {
        quizLoadProgress.visibility = View.GONE
        val options = arrayOf(quiz.solution, quiz.distractor1, quiz.distractor2, quiz.distractor3)
        options.shuffle()
        firstPart.text = quiz.part1
        lastPart.text = quiz.part2
        solutionView.text = quiz.solution
    }


    private fun getImageOrientation(path: String): Number {

        var exif: ExifInterface?
        try {
            exif = ExifInterface(path)
            exif.let {
                var rotation = 0
                val orientation: Int = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> rotation = 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> rotation = 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> rotation = 270
                }
                return rotation
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return 0
    }

    private fun getCaptureTime(path: String): String? {

        var exif: ExifInterface?
        try {
            exif = ExifInterface(path)
            return exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)

        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    private fun scaleAndRotateImage(bitmap: Bitmap, orientation: Number, maxDim: Int): Bitmap? {
        val matrix = Matrix()
        val scale = maxDim.div(bitmap.width.coerceAtLeast(bitmap.height).toFloat())
        matrix.preScale(scale, scale)
        matrix.postRotate(orientation.toFloat())

        try {
            val bmRotated = Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.width,
                bitmap.height,
                matrix,
                true
            )
            bitmap.recycle()
            return bmRotated
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
            return null
        }

    }

    private fun onNewPhotoQuizClicked() {

        val selectPhotoIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(selectPhotoIntent, ODT_REQUEST_LIBRARY_PHOTO)
    }

    private fun Array<String>.shuffle() {
        val rnd = Random()
        // Fisher-Yates shuffle algorithm
        for (i in this.size - 1 downTo 1) {
            val j = rnd.nextInt(i + 1)
            val temp = this[i]
            this[i] = this[j]
            this[j] = temp
        }
    }
}