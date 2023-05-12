package de.lmu.personalflashcards.datahandling

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import de.lmu.personalflashcards.model.Quiz
import de.lmu.personalflashcards.model.QuizDao

class PersonalFlashcardsRepository(private val quizDao: QuizDao) {
    val allQuizzes: LiveData<List<Quiz>> = quizDao.getAllQuizzes()
    val quizCount: LiveData<Int> = quizDao.countQuizzes()

    suspend fun insert(quiz: Quiz) {
        quizDao.insert(quiz)
    }

    fun getQuiz(id: Long): LiveData<List<Quiz>> {
        return quizDao.getQuiz(id)
    }

    fun getQuizByImage(imagePath: String): LiveData<List<Quiz>> {
        return quizDao.getQuizByImage(imagePath)
    }

    suspend fun deleteQuiz(quiz: Quiz) {
        quizDao.deleteQuiz(quiz)
    }

}