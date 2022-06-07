package com.example.android.commitcontent.ime.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.example.android.commitcontent.ime.R
import com.example.android.commitcontent.ime.model.MemeImages

class Datasource (context: Context){


    var uriList: MutableList<Uri> = mutableListOf()

    fun setData(uris: MutableList<Uri>){
        for (uri in uris){
            uriList.add(uri)
        }

    }

    fun loadAffirmations(): List<Uri> {
        return uriList

        /*
        return listOf<MemeImages>(
            MemeImages(R.string.affirmation1),
            MemeImages(R.string.affirmation2),
            MemeImages(R.string.affirmation3),
            MemeImages(R.string.affirmation4),
            MemeImages(R.string.affirmation5),
            MemeImages(R.string.affirmation6),
            MemeImages(R.string.affirmation7),
            MemeImages(R.string.affirmation8),
            MemeImages(R.string.affirmation9),
            MemeImages(R.string.affirmation10)
        )*/
    }
}