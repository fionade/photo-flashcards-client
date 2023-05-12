package de.lmu.personalflashcards.datahandling

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import de.lmu.personalflashcards.model.LogDao
import de.lmu.personalflashcards.model.LogEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LogViewModel(application: Application) : AndroidViewModel(application) {
    var logDao: LogDao = PersonalFlashcardsDatabase.getDatabase(application).logDao()

    val averageAttempts: LiveData<Float>
    val lastTrainingDate: LiveData<LogEntry>

    init {
        averageAttempts = logDao.averageAttempts()
        lastTrainingDate = logDao.getLastTrainingDate()
    }

    fun insert(logEntry: LogEntry) = viewModelScope.launch(Dispatchers.IO) {
        logDao.insert(logEntry)
    }

    fun insertMultiple(logEntries: List<LogEntry>) = viewModelScope.launch(Dispatchers.IO) {
        logDao.insertMultiple(logEntries)
    }
}