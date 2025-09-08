package com.example.dailymovie.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.dailymovie.R
import com.example.dailymovie.models.VideoModel
import com.example.dailymovie.utils.Trailers

class VideoAdapter(private val context: Context, private val videoList: List<VideoModel>) :
    RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_video, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = videoList[position]

        // YouTube ya no deja reproducir dentro de un WebView, asi que se enseña la miniatura
        // y al tocarla se abre el trailer en YouTube. Ver utils/Trailers.
        Glide.with(context)
            .load(Trailers.miniatura(video.key))
            .placeholder(R.drawable.ic_baseline_image_24)
            .error(R.drawable.ic_baseline_image_24)
            .into(holder.miniatura)

        holder.contenedor.setOnClickListener {
            Trailers.abrir(context, video.key)
        }

        holder.txtVideoName.text = video.name
    }

    override fun getItemCount(): Int {
        return videoList.size
    }

    class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val contenedor: FrameLayout = itemView.findViewById(R.id.trailerContainer)
        val miniatura: ImageView = itemView.findViewById(R.id.trailerThumbnail)
        val txtVideoName: TextView = itemView.findViewById(R.id.txtVideoName)
    }
}
