package de.lmu.personalflashcards.model

import kotlinx.serialization.Serializable

@Serializable
data class SerializableQuiz(val english: String, val german: String,
                            val part1: String, val part2: String,
                            val solution: String, val distractor1: String,
                            val distractor2: String, val distractor3: String,
                            val type: String, var imagePath: String = "", var thumbnailPath: String = "",
                            var predefined: Boolean = false, var id: Long = -1) {

    fun toQuiz(): Quiz {
        return Quiz(this.english, this.german, this.part1, this.part2, this.solution, this.distractor1,
            this.distractor2, this.distractor3, this.type, this.imagePath, this.thumbnailPath, this.predefined)
    }
}