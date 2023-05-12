package de.lmu.personalflashcards.uielements

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.lmu.personalflashcards.R
import de.lmu.personalflashcards.model.Quiz
import java.io.File
import java.io.IOException


class CardListAdapter(private val onCardClickListener: OnCardClickListener, private val context: Context) : RecyclerView.Adapter<CardListAdapter.MyViewHolder>() {

    private var quizList = emptyList<Quiz>()

    inner class MyViewHolder(inflater: LayoutInflater, parent: ViewGroup) :
        RecyclerView.ViewHolder(inflater.inflate(R.layout.card_list_item, parent, false)) {

        private var cardImage: ImageView = itemView.findViewById(R.id.card_item_image)
        private var cardQuiz: TextView = itemView.findViewById(R.id.card_item_quiz)

        fun bind(quiz: Quiz) {
//            Picasso.get().load(quiz.imagePath).into(cardImage)
            if (quiz.predefined) {
                try {
                    context.assets.open(quiz.thumbnailPath).use {
                        cardImage.setImageDrawable(Drawable.createFromStream(it, null))
                    }
                }
                catch (e: IOException) {
                    Log.d(PhotoQuizFragment.TAG, "could not find image")
                }
            }
            else {
                val pictureFile = File(quiz.thumbnailPath)
                val bitmap = BitmapFactory.decodeFile(pictureFile.absolutePath)
                cardImage.setImageBitmap(bitmap)
            }
            cardQuiz.text = quiz.part1 + "..."

            itemView.setOnClickListener {
                onCardClickListener.onCardClicked(quiz.id)
            }

        }

    }

    interface OnCardClickListener {
        fun onCardClicked(quizId: Long)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return MyViewHolder(inflater, parent)
    }

    override fun getItemCount(): Int {
        return quizList.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(quizList[position])
    }

    internal fun setQuizzes(quizzes: List<Quiz>) {
        this.quizList = quizzes
        notifyDataSetChanged()
    }
}