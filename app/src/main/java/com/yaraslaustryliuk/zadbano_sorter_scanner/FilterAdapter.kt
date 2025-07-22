package com.yaraslaustryliuk.zadbano_sorter_scanner

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FilterAdapter(
    private val prefixes: MutableList<String>,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<FilterAdapter.FilterViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_filter, parent, false)
        return FilterViewHolder(view)
    }

    override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
        val prefix = prefixes[position]
        holder.bind(prefix, position)
    }

    override fun getItemCount(): Int = prefixes.size

    inner class FilterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val prefixTextView: TextView = itemView.findViewById(R.id.prefixTextView)
        private val deleteIcon: ImageView = itemView.findViewById(R.id.deleteIcon)

        fun bind(prefix: String, position: Int) {
            prefixTextView.text = prefix

            deleteIcon.setOnClickListener {
                val adapterPosition = bindingAdapterPosition
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onDeleteClick(adapterPosition)
                }
            }
        }
    }
}