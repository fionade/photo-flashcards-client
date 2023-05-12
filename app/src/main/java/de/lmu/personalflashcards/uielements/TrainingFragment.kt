package de.lmu.personalflashcards.uielements

import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import de.lmu.personalflashcards.Logger
import de.lmu.personalflashcards.R
import de.lmu.personalflashcards.datahandling.QuizViewModel
import de.lmu.personalflashcards.model.Quiz
import kotlinx.android.synthetic.main.fragment_quiz.*
import kotlinx.android.synthetic.main.fragment_train.*
import kotlinx.android.synthetic.main.fragment_train.view.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.util.*

class TrainingFragment: Fragment() {

    companion object {
        const val NUMBER_OF_QUESTIONS = 15
    }

    // Logging
    private lateinit var logger: Logger

    private var currentIndex = -1
    private var correctCounter = 0
    private var incorrectCounter = 0
    private lateinit var quizViewModel: QuizViewModel
    private lateinit var quizList: List<Quiz>

    private lateinit var optionViewList: List<Chip>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_train, container, false)

        // Logging
        logger = Logger(requireContext())
        lifecycleScope.launch {
            logger.addLogMessage("training_started", "")
        }

        optionViewList = listOf(root.trainOptionChip1, root.trainOptionChip2, root.trainOptionChip3, root.trainOptionChip4)

        quizViewModel = ViewModelProvider(this).get(QuizViewModel::class.java)
        quizViewModel.allQuizzes.observe(viewLifecycleOwner, Observer { quizzes ->
            if (quizzes.isNotEmpty()) {
                // shuffle quizzes and truncate to X cards per round
                quizList = quizzes.shuffled().subList(0, quizzes.size.coerceAtMost(NUMBER_OF_QUESTIONS))
                currentIndex = 0
                root.correctCountView.text = getString(R.string.correct, correctCounter)
                root.incorrectCountView.text = getString(R.string.incorrect, incorrectCounter)

                setQuiz()
            }
        })

        root.difficultCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && quizList.isNotEmpty() && quizList.size >= currentIndex) {
                logger.addQuizLogMessage("difficult", "true", quizList[currentIndex].id)
            }
        }

        root.trainNext.setOnClickListener { nextQuiz() }

        return root
    }

    private fun setQuiz() {

        val quiz = quizList[currentIndex]

        if (quiz.predefined) {
            try {
                requireContext().assets.open(quiz.imagePath).use {
                    trainImage.setImageDrawable(Drawable.createFromStream(it, null))
                }
            }
            catch (e: IOException) {
                Log.d(PhotoQuizFragment.TAG, "could not find image")
            }
        }
        else {
            val pictureFile = File(quiz.imagePath)
            val bitmap = BitmapFactory.decodeFile(pictureFile.absolutePath)
            trainImage.setImageBitmap(bitmap)
        }

        val options = arrayOf(quiz.solution, quiz.distractor1, quiz.distractor2, quiz.distractor3)
        options.shuffle()
        trainFirstPart.text = quiz.part1
        trainLastPart.text = quiz.part2
        trainOptionChip1.text = options[0]
        trainOptionChip2.text = options[1]
        trainOptionChip3.text = options[2]
        trainOptionChip4.text = options[3]

        // reset radio button colors
        if (Build.VERSION.SDK_INT >= 23) {
            optionViewList.forEach { it.setTextColor(resources.getColor(R.color.colorQuizText, activity?.application?.theme)) }
        }
        else {
            optionViewList.forEach { it.setTextColor(resources.getColor(R.color.colorQuizText)) }
        }

        optionViewList.forEach { it ->
            it.setOnClickListener { onOptionSelected(it) }
            it.isCheckable = true
        }

        trainNext.visibility = View.INVISIBLE
        difficultCheckBox.isChecked = false
        difficultCheckBox.visibility = View.INVISIBLE
        trainErrorIndicator.visibility = View.GONE
        trainSuccessIndicator.visibility = View.GONE
    }

    private fun onOptionSelected(button: View) {
        val view = button as Chip

        if (view.isCheckable) {
            if(view.text == quizList[currentIndex].solution) {
                lifecycleScope.launch {
                    logger.addQuizLogMessage("answer_selected", "correct: ${view.text}", quizList[currentIndex].id)
                }

                if (Build.VERSION.SDK_INT >= 23) {
                    view.setTextColor(resources.getColor(R.color.colorRightAnswer, activity?.theme))
                }
                else {
                    view.setTextColor(resources.getColor(R.color.colorRightAnswer))
                }

                correctCounter++
                correctCountView.text = getString(R.string.correct, correctCounter)

                optionViewList.forEach {
                    it.isChecked = false
                    it.isCheckable = false
                }
                trainNext.visibility = View.VISIBLE
                difficultCheckBox.visibility = View.VISIBLE
                trainSuccessIndicator.visibility = View.VISIBLE
                trainErrorIndicator.visibility = View.GONE
            }
            else {
                lifecycleScope.launch {
                    logger.addQuizLogMessage("answer_selected", "incorrect: ${view.text}", quizList[currentIndex].id)
                }
                if (Build.VERSION.SDK_INT >= 23) {
                    view.setTextColor(resources.getColor(R.color.colorWrongAnswer, activity?.theme))
                }
                else {
                    view.setTextColor(resources.getColor(R.color.colorWrongAnswer))
                }
                incorrectCounter++
                incorrectCountView.text = getString(R.string.incorrect, incorrectCounter)
                trainErrorIndicator.visibility = View.VISIBLE
            }

            view.isCheckable = false
        }

    }

    private fun nextQuiz() {
        if (currentIndex < quizList.size - 1) {

            currentIndex++
            setQuiz()

        }
        else {

            val builder: AlertDialog.Builder? = activity?.let {
                AlertDialog.Builder(it)
            }

            builder?.apply {
                setMessage(getString(R.string.training_done, correctCounter + incorrectCounter, correctCounter))
                setTitle(R.string.training_done_title)
                setPositiveButton(R.string.ok
                ) { _, _ ->
                    lifecycleScope.launch {
                        logger.addLogMessage("training_done", "correct: $correctCounter, incorrect: $incorrectCounter")
                    }
                    findNavController().navigateUp()
                }
            }

            builder?.create()?.show()
        }
    }

    override fun onPause() {

        lifecycleScope.launch {
            logger.addLogMessage("training_stopped", "correct: $correctCounter, incorrect: $incorrectCounter")
        }

        super.onPause()
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