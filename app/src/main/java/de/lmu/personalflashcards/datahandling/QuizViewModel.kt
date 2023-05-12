package de.lmu.personalflashcards.datahandling

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import de.lmu.personalflashcards.model.Quiz
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class QuizViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PersonalFlashcardsRepository

    val allQuizzes: LiveData<List<Quiz>>
    var oneQuiz: LiveData<List<Quiz>>
    var quizCount: LiveData<Int>
    var quizByImage: LiveData<List<Quiz>>

    init {
        val quizDao = PersonalFlashcardsDatabase.getDatabase(application).quizDao()
        repository = PersonalFlashcardsRepository(quizDao)
        allQuizzes = repository.allQuizzes
        oneQuiz = repository.getQuiz(-1)
        quizCount = repository.quizCount
        quizByImage = repository.getQuizByImage("")
    }

    fun insert(quiz: Quiz) = viewModelScope.launch(Dispatchers.IO) {
        repository.insert(quiz)
    }

    fun getQuiz(id: Long) {
        viewModelScope.launch {
            oneQuiz = repository.getQuiz(id)
        }
    }

    fun getQuizByImage(imagePath: String) {
        viewModelScope.launch {
            quizByImage = repository.getQuizByImage(imagePath)
        }
    }

    fun deleteQuiz(quiz: Quiz) {
        viewModelScope.launch {
            repository.deleteQuiz(quiz)
        }
    }
}