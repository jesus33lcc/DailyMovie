package com.example.dailymovie.adapters


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.dailymovie.R
import com.example.dailymovie.models.MovieModel
import com.example.dailymovie.utils.Constantes

class MovieAdapter(
    var listMovies:ArrayList<MovieModel>
): RecyclerView.Adapter<MovieAdapter.MovieViewHolder>() {
    class MovieViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.txt_titleMovie)
        val year: TextView = itemView.findViewById(R.id.txt_yearMovie)
        val poster: ImageView = itemView.findViewById(R.id.img_posterMovie)
        val tipo: TextView = itemView.findViewById(R.id.txt_tipoMovie)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_movie_list, parent, false)
        return MovieViewHolder(view)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        val movie = listMovies[position]
        holder.title.text=movie.title
        holder.year.text=movie.releaseDate.take(4)
        holder.tipo.text=""
        Glide.with(holder.itemView.context).load(Constantes.IMAGE_URL+movie.posterPath).placeholder(R.drawable.ic_baseline_image_24).error(R.drawable.ic_baseline_image_24).into(holder.poster)
    }

    override fun getItemCount(): Int {
        return listMovies.size
    }

}