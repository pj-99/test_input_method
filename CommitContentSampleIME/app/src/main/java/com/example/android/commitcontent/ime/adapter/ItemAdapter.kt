package com.example.android.commitcontent.ime.adapter

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.android.commitcontent.ime.R
import com.example.android.commitcontent.ime.model.MemeImages


class ItemAdapter(private val context: Context, private val dataset: List<Uri>, val memeListener: MemeListener)
    : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

    companion object{
        const val TAG = "ItemAdapter"
    }
    class ItemViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.item_title)
        val imgView: ImageView = view.findViewById(R.id.item_image)
    }

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ItemViewHolder {
        // create a new view
        val adapterLayout = LayoutInflater.from(p0.context)
            .inflate(R.layout.list_item, p0, false)

        return ItemViewHolder(adapterLayout)

    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = dataset[position]
        holder.textView.text =  "LULULU${position}"

        Glide.with(context)
            .load(dataset[position])
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(holder.imgView)

        holder.imgView.setOnClickListener {
            memeListener.onClick(dataset[position])
        }

    }

    override fun getItemCount(): Int {
        return dataset.size
    }


}

class MemeListener(val clickListener: (imgUri: Uri) -> Unit) {
    fun onClick(imgUri: Uri)  = clickListener(imgUri)
}