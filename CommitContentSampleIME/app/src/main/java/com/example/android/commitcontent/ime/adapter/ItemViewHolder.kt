package com.example.android.commitcontent.ime.adapter

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.android.commitcontent.ime.R

class ItemViewHolder(private val view: View): RecyclerView.ViewHolder(view) {
    val textView: TextView = view.findViewById(R.id.item_title)
    val imageView: ImageView = view.findViewById(R.id.item_image)
}