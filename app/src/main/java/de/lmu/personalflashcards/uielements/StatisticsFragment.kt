package de.lmu.personalflashcards.uielements

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.format.DateUtils
import android.text.method.LinkMovementMethod
import android.text.method.MovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import de.lmu.personalflashcards.Logger
import de.lmu.personalflashcards.R
import de.lmu.personalflashcards.datahandling.LogViewModel
import de.lmu.personalflashcards.datahandling.QuizViewModel
import kotlinx.android.synthetic.main.fragment_statistics.view.*
import kotlin.math.log

class StatisticsFragment: Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_statistics, container, false)

        // Logging
        val logger = Logger(requireContext())
        logger.addLogMessage("opened_statistics", "")

        val quizViewModel = ViewModelProvider(this).get(QuizViewModel::class.java)
        quizViewModel.quizCount.observe(viewLifecycleOwner, Observer { value ->
            root.cardsAdded.text = getString(R.string.number_of_cards_added, value)
            logger.addLogMessage("number_cards_added", value.toString())
            if (value == 0) {
                root.addCardsHint.visibility = View.VISIBLE
            }
        } )

        val logViewModel = ViewModelProvider(this).get(LogViewModel::class.java)

        // get starting time of previous training session
        logViewModel.lastTrainingDate.observe(viewLifecycleOwner) { logEntry ->
            activity?.runOnUiThread {
                if (logEntry != null && logEntry.timestamp > 0) {
                    root.lastTrainingRound.text = getString(R.string.days_since_training, DateUtils.getRelativeTimeSpanString(logEntry.timestamp))
                }
                else {
                    root.lastTrainingRound.text = getString(R.string.days_since_training, "-1")
                }
            }
        }

        // average attempts per cards
        logViewModel.averageAttempts.observe(viewLifecycleOwner) { attempts ->
            activity?.runOnUiThread {
                val attemptsString = "%.${1}f".format(attempts)
                root.attemptsPerCard.text = getString(R.string.attempts_per_card, attemptsString)
            }
        }

        activity?.let {
            val sharedPreferences = it.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)
            val participantId = sharedPreferences.getString("user_key", "")
            root.participantId.text = getString(R.string.participantId).format(
                participantId?.substring(IntRange(0,  7.coerceAtMost(
                    participantId.length - 1
                ))))
        }

        return root
    }
}