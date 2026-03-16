package it.fonsolo.muzeiroma

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class LogAdapter : ListAdapter<LogEntity, LogAdapter.LogViewHolder>(LogDiffCallback()) {

    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val log = getItem(position)
        holder.bind(log, dateFormat)
    }

    class LogViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val text1: TextView = view.findViewById(android.R.id.text1)
        private val text2: TextView = view.findViewById(android.R.id.text2)

        fun bind(log: LogEntity, dateFormat: SimpleDateFormat) {
            val time = dateFormat.format(Date(log.timestamp))
            text1.text = "[$time] ${log.level}"
            text2.text = log.message
            
            when (log.level) {
                "ERROR" -> {
                    text1.setTextColor(Color.RED)
                    text2.setTextColor(Color.RED)
                }
                "WARN" -> {
                    text1.setTextColor(Color.parseColor("#FF8C00")) // DarkOrange
                    text2.setTextColor(Color.BLACK)
                }
                else -> {
                    text1.setTextColor(Color.GRAY)
                    text2.setTextColor(Color.BLACK)
                }
            }
            text2.textSize = 12f
        }
    }

    class LogDiffCallback : DiffUtil.ItemCallback<LogEntity>() {
        override fun areItemsTheSame(oldItem: LogEntity, newItem: LogEntity): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: LogEntity, newItem: LogEntity): Boolean = oldItem == newItem
    }
}
