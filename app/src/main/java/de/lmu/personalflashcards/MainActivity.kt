package de.lmu.personalflashcards

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import de.lmu.personalflashcards.datahandling.PersonalFlashcardsDatabase
import de.lmu.personalflashcards.model.Quiz
import de.lmu.personalflashcards.model.SerializableQuiz
import de.lmu.personalflashcards.uielements.OnboardingFragment
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.util.*


class MainActivity : AppCompatActivity() {

    // Logging
    private lateinit var logger: Logger

    companion object {
        const val TAG = "Main Activity"
        const val BASE_URL = "<base_url>/%s" // TODO replace server URL. HTTPS is mandatory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        logger = Logger(this)
        logger.addLogMessage("app_started", "")

        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.findNavController()
        navView.setupWithNavController(navController)

        setSupportActionBar(main_toolbar)
        val appBarConfiguration = AppBarConfiguration(setOf(R.id.navigation_list, R.id.navigation_train_home, R.id.navigation_statistics))
        main_toolbar.setupWithNavController(navController, appBarConfiguration)

        navController.addOnDestinationChangedListener { _, destination, _ ->

            if(destination.id == R.id.navigation_train || destination.id == R.id.navigation_quiz || destination.id == R.id.navigation_onboarding){
                nav_view.visibility = View.GONE
                if (destination.id == R.id.navigation_onboarding) {
                    main_toolbar.navigationIcon = null
                }
            }else{
                nav_view.visibility = View.VISIBLE
            }

        }

        // set a user ID that is sent with server requests
        val sharedPreferences = getSharedPreferences(
            getString(R.string.preference_file_key), Context.MODE_PRIVATE)
        val userKey = sharedPreferences.getString("user_key", "")
        if (userKey.isNullOrBlank()) {
            sharedPreferences.edit()
                .putString("user_key", "none")
                .apply()
        }

        // trigger daily notifications
        ReminderService().setAlarm(this)

        // is the app started for the first time?
        // Then load predefined quizzes
        val firstStart = sharedPreferences.getBoolean("is_first_start", true)

        if (firstStart) {

            // Database setup: load data from json file
            val jsonString = this.assets.open("examples.json").bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)
            val serializableQuizzesJson = jsonObject.getJSONArray("quizzes")

            val quizzes = ArrayList<Quiz>()
            for (i in 0 until serializableQuizzesJson.length()) {
                val serializableQuiz = Json.decodeFromString<SerializableQuiz>(serializableQuizzesJson[i].toString())
                val quiz = serializableQuiz.toQuiz()
                quizzes.add(quiz)
            }

            GlobalScope.launch {
                val quizDao = PersonalFlashcardsDatabase.getDatabase(application).quizDao()
                quizDao.insertMultiple(quizzes)
            }

            sharedPreferences.edit()
                .putString("condition", "predefined")
                .putBoolean("is_first_start", false)
                .apply()
        }

    }

    override fun onPause() {
        logger.addLogMessage("app_paused", "")
        super.onPause()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // On the consent screen, back navigation should not be possible
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navHostFragment.childFragmentManager.primaryNavigationFragment?.let {fragment->
                if (fragment !is OnboardingFragment) {
                    super.onBackPressed()
                }
            }
    }



}
