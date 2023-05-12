package de.lmu.personalflashcards.uielements

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import de.lmu.personalflashcards.R
import de.lmu.personalflashcards.model.Quiz
import java.io.IOException

class PhotoListAdapter(private val onCardClickListener: OnCardClickListener, private val context: Context): RecyclerView.Adapter<PhotoListAdapter.MyViewHolder>() {

    private var photoList = emptyList<String>()

    inner class MyViewHolder(inflater: LayoutInflater, parent: ViewGroup) :
        RecyclerView.ViewHolder(inflater.inflate(R.layout.photo_list_item, parent, false)) {

        private var cardImage: ImageView = itemView.findViewById(R.id.photoItemImage)

        fun bind(imagePath: String) {
            // the thumbnail images are included as assets, as they are very small
            try {
                context.assets.open(imagePath).use {
                    cardImage.setImageDrawable(Drawable.createFromStream(it, null))
                    cardImage.setOnClickListener {
                        onCardClickListener.onCardClicked(imagePath)
                    }
                }
            }
            catch (e: IOException) {
                Log.d(PhotoQuizFragment.TAG, "could not find image")
            }
        }
    }

    interface OnCardClickListener {
        fun onCardClicked(imagePath: String)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return MyViewHolder(inflater, parent)
    }

    override fun getItemCount(): Int {
        return photoList.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(photoList[position])
    }

    internal fun setPhotos(photos: List<String>) {
        this.photoList = photos
        notifyDataSetChanged()
    }
}