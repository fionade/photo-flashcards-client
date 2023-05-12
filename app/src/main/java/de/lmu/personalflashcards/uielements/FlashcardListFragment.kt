package de.lmu.personalflashcards.uielements

import android.content.Context
import android.os.Bundle
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
import com.google.android.material.snackbar.Snackbar
import de.lmu.personalflashcards.Logger
import de.lmu.personalflashcards.R
import de.lmu.personalflashcards.datahandling.QuizViewModel
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_list.view.*
import kotlinx.coroutines.launch

class FlashcardListFragment: Fragment(), CardListAdapter.OnCardClickListener {

    // Logging
    private lateinit var logger: Logger

    private lateinit var cardListView: RecyclerView
    private lateinit var cardListAdapter: RecyclerView.Adapter<*>
    private lateinit var cardListViewManager: RecyclerView.LayoutManager

    private lateinit var quizViewModel: QuizViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_list, container, false)

        // Logging
        logger = Logger(requireContext())

        cardListViewManager = GridLayoutManager(requireActivity(), 3)
        cardListAdapter = CardListAdapter(this, requireContext())

        cardListView = root.card_list as RecyclerView
        cardListView.apply {
            setHasFixedSize(true)
            layoutManager = cardListViewManager
            adapter = cardListAdapter
        }

        quizViewModel = ViewModelProvider(this).get(QuizViewModel::class.java)

        quizViewModel.allQuizzes.observe(viewLifecycleOwner, Observer {
                quizzes -> quizzes?.let { (cardListAdapter as CardListAdapter).setQuizzes(it) }
            logger.addLogMessage("quiz_list", "length: ${quizzes.size}")
        })

        // button for adding new photos
        root.new_quiz.setOnClickListener {

            context?.let {
                val sharedPreferences = it.getSharedPreferences(
                    it.getString(R.string.preference_file_key), Context.MODE_PRIVATE)
                val condition = sharedPreferences.getString("condition", "")

                if (condition == "predefined") {
                    findNavController().navigate(R.id.navigation_new)
                    logger.addLogMessage("add_quiz_clicked", condition)
                }
                else if (condition == "personal") {
                    val bundle = bundleOf("openPicker" to true)
                    findNavController().navigate(R.id.navigation_quiz, bundle)
                    logger.addLogMessage("add_quiz_clicked", condition)
                }
                else {
                    Snackbar.make(root, R.string.could_not_initialise, Snackbar.LENGTH_SHORT).show()
                }

            }

        }

        return root
    }

    override fun onCardClicked(quizId: Long) {
        // Open Quiz
        val bundle = bundleOf("openPicker" to false, "showQuiz" to quizId)
        lifecycleScope.launch {
            logger.addQuizLogMessage("show_quiz", "existing", quizId)
        }
        findNavController().navigate(R.id.navigation_quiz, bundle)
    }
}