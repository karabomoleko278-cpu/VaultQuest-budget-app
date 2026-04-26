package com.iie.vaultquest.ui

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.iie.vaultquest.R
import com.iie.vaultquest.data.Category
import com.iie.vaultquest.data.Entry
import java.io.File
import java.text.NumberFormat
import java.util.*

class EntryAdapter(
    private val entries: List<Entry>,
    private val categories: Map<Long, Category>
) : RecyclerView.Adapter<EntryAdapter.EntryViewHolder>() {

    class EntryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val desc: TextView = view.findViewById(R.id.txtDescription)
        val cat: TextView = view.findViewById(R.id.txtCategory)
        val amount: TextView = view.findViewById(R.id.txtAmount)
        val time: TextView = view.findViewById(R.id.txtTime)
        val icon: ImageView = view.findViewById(R.id.imgEntry)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_entry, parent, false)
        return EntryViewHolder(view)
    }

    override fun onBindViewHolder(holder: EntryViewHolder, position: Int) {
        val entry = entries[position]
        holder.desc.text = entry.description
        val categoryName = categories[entry.categoryId]?.name ?: "Uncategorized"
        holder.cat.text = "Category: $categoryName"
        holder.amount.text = "R ${String.format("%.2f", entry.amount)}"
        holder.time.text = "${entry.startTime} - ${entry.endTime}"
        
        // Simple gamification: change color based on amount
        if (entry.amount > 1000) {
            holder.amount.setTextColor(android.graphics.Color.RED)
        } else {
            holder.amount.setTextColor(holder.itemView.context.getColor(R.color.secondary))
        }

        if (entry.photoPath != null) {
            holder.icon.visibility = View.VISIBLE
            holder.icon.setImageURI(Uri.fromFile(File(entry.photoPath)))
        } else {
            holder.icon.visibility = View.GONE
        }
    }

    override fun getItemCount() = entries.size
}
