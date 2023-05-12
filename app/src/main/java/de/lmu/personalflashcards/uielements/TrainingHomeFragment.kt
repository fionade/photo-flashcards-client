package de.lmu.personalflashcards.uielements

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import de.lmu.personalflashcards.R
import kotlinx.android.synthetic.main.fragment_train_home.view.*

class TrainingHomeFragment: Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_train_home, container, false)

        root.startTrainingButton.setOnClickListener {
            findNavController().navigate(R.id.action_train_home_to_train)
        }

        return root
    }
}