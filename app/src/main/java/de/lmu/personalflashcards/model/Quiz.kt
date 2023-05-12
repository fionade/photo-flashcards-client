package de.lmu.personalflashcards.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.room.*

@Entity(tableName = "quiz", indices = arrayOf(Index(value = ["imagePath"], unique = true)))
data class Quiz(val english: String, val german: String,
                val part1: String, val part2: String,
                val solution: String, val distractor1: String,
                val distractor2: String, val distractor3: String,
                val type: String, var imagePath: String,
                var thumbnailPath: String, var predefined: Boolean = false) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
}

@Dao
interface QuizDao {

    @Query("SELECT * FROM quiz ORDER BY id DESC")
    fun getAllQuizzes(): LiveData<List<Quiz>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(quiz: Quiz): Long

    @Query("SELECT * FROM quiz WHERE id = (:id)")
    fun getQuiz(id: Long): LiveData<List<Quiz>>

    @Query("SELECT * FROM quiz WHERE imagePath = (:imagePath)")
    fun getQuizByImage(imagePath: String): LiveData<List<Quiz>>

    @Delete
    suspend fun deleteQuiz(quiz: Quiz)

    @Query("SELECT COUNT(english) FROM quiz WHERE predefined = 0")
    fun countQuizzes(): LiveData<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMultiple(quizzes: List<Quiz>)
}