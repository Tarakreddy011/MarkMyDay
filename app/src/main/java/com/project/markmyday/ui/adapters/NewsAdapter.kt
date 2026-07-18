package com.project.markmyday.ui.adapters

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.project.markmyday.data.model.GNewsArticle
import com.project.markmyday.databinding.ItemNewsBinding
import com.project.markmyday.R

class NewsAdapter : ListAdapter<GNewsArticle, NewsAdapter.NewsViewHolder>(NewsDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val binding = ItemNewsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NewsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class NewsViewHolder(private val binding: ItemNewsBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(article: GNewsArticle) {
            binding.tvNewsTitle.text = article.title ?: binding.root.context.getString(R.string.no_title)
            binding.tvNewsDescription.text = article.description ?: binding.root.context.getString(R.string.no_description)
            binding.tvPubDate.text = article.publishedAt ?: ""

            Glide.with(binding.ivNewsImage.context)
                .load(article.image)
                .placeholder(android.R.color.darker_gray)
                .error(android.R.color.darker_gray)
                .into(binding.ivNewsImage)

            binding.root.setOnClickListener {
                article.url?.let { url ->
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    binding.root.context.startActivity(intent)
                }
            }
        }
    }

    class NewsDiffCallback : DiffUtil.ItemCallback<GNewsArticle>() {
        override fun areItemsTheSame(oldItem: GNewsArticle, newItem: GNewsArticle): Boolean {
            return oldItem.url == newItem.url
        }

        override fun areContentsTheSame(oldItem: GNewsArticle, newItem: GNewsArticle): Boolean {
            return oldItem == newItem
        }
    }
}
