package com.yaraslaustryliuk.zadbano_sorter_scanner

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class BarcodeAdapter(
    private val onDeleteClick: (Int) -> Unit,
    private val onEditCommentClick: (Int, String) -> Unit
) : ListAdapter<BarcodeItem, BarcodeAdapter.BarcodeViewHolder>(BarcodeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BarcodeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_barcode, parent, false)
        return BarcodeViewHolder(view)
    }

    override fun onBindViewHolder(holder: BarcodeViewHolder, position: Int) {
        val barcodeItem = getItem(position)
        holder.bind(barcodeItem, position)
    }

    inner class BarcodeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val barcodeCodeTextView: TextView = itemView.findViewById(R.id.barcodeCodeTextView)
        private val barcodeCommentTextView: TextView = itemView.findViewById(R.id.barcodeCommentTextView)
        private val barcodeTimestampTextView: TextView = itemView.findViewById(R.id.barcodeTimestampTextView)
        private val deleteIcon: ImageView = itemView.findViewById(R.id.deleteIcon)
        private val editCommentIcon: ImageView = itemView.findViewById(R.id.editCommentIcon)

        fun bind(barcodeItem: BarcodeItem, position: Int) {
            // Показываем только код без нумерации
            barcodeCodeTextView.text = barcodeItem.code

            // Комментарий
            if (barcodeItem.hasComment()) {
                barcodeCommentTextView.text = "💬 ${barcodeItem.comment}"
                barcodeCommentTextView.visibility = View.VISIBLE
            } else {
                barcodeCommentTextView.visibility = View.GONE
            }

            // Время сканирования
            barcodeTimestampTextView.text = barcodeItem.getFormattedTimestamp()

            // Обработчики кликов
            deleteIcon.setOnClickListener {
                val adapterPosition = bindingAdapterPosition
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onDeleteClick(adapterPosition)
                }
            }

            editCommentIcon.setOnClickListener {
                val adapterPosition = bindingAdapterPosition
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onEditCommentClick(adapterPosition, barcodeItem.comment)
                }
            }
        }
    }

    private class BarcodeDiffCallback : DiffUtil.ItemCallback<BarcodeItem>() {
        override fun areItemsTheSame(oldItem: BarcodeItem, newItem: BarcodeItem): Boolean {
            return oldItem.code == newItem.code && oldItem.timestamp == newItem.timestamp
        }

        override fun areContentsTheSame(oldItem: BarcodeItem, newItem: BarcodeItem): Boolean {
            return oldItem == newItem
        }
    }
}