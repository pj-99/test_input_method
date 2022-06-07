/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.commitcontent.ime

import android.app.AppOpsManager
import android.content.ClipDescription
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import androidx.annotation.RawRes
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.android.commitcontent.ime.adapter.ItemAdapter
import com.example.android.commitcontent.ime.adapter.MemeListener
import com.example.android.commitcontent.ime.data.Datasource
import java.io.*
import java.net.URI
import java.util.concurrent.TimeUnit

class ImageKeyboard : InputMethodService() {
    private var mPngFile: File? = null
    private var mGifFile: File? = null
    private var mWebpFile: File? = null
    private var mGifButton: Button? = null
    private var mPngButton: Button? = null
    private var mWebpButton: Button? = null
    private var mEditText: EditText? = null
    private var mgetImageButton: Button? = null


    var uriList: MutableList<Uri> = mutableListOf()
    //val bucket_id  = 1384292870
    val bucket_name ="testMeme"
    private fun isCommitContentSupported(
        editorInfo: EditorInfo?, mimeType: String
    ): Boolean {
        if (editorInfo == null) {
            return false
        }
        val ic = currentInputConnection ?: return false
        if (!validatePackageName(editorInfo)) {
            return false
        }
        val supportedMimeTypes = EditorInfoCompat.getContentMimeTypes(editorInfo)
        for (supportedMimeType in supportedMimeTypes) {
            if (ClipDescription.compareMimeTypes(mimeType, supportedMimeType)) {
                return true
            }
        }
        return false
    }

    private fun doCommitContent(
        description: String, mimeType: String,
        file: File?, uri:Uri? = null
    ) {
        val editorInfo = currentInputEditorInfo

        // Validate packageName again just in case.
        if (!validatePackageName(editorInfo)) {
            return
        }
        var contentUri: Uri

        if(uri!= null)
            contentUri = uri
        else
            contentUri = FileProvider.getUriForFile(this, AUTHORITY, file!!)

        // As you as an IME author are most likely to have to implement your own content provider
        // to support CommitContent API, it is important to have a clear spec about what
        // applications are going to be allowed to access the content that your are going to share.
        val flag: Int
        if (Build.VERSION.SDK_INT >= 25) {
            // On API 25 and later devices, as an analogy of Intent.FLAG_GRANT_READ_URI_PERMISSION,
            // you can specify InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION to give
            // a temporary read access to the recipient application without exporting your content
            // provider.
            flag = InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION
        } else {
            // On API 24 and prior devices, we cannot rely on
            // InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION. You as an IME author
            // need to decide what access control is needed (or not needed) for content URIs that
            // you are going to expose. This sample uses Context.grantUriPermission(), but you can
            // implement your own mechanism that satisfies your own requirements.
            flag = 0
            try {
                // TODO: Use revokeUriPermission to revoke as needed.
                grantUriPermission(
                    editorInfo.packageName, contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                Log.e(
                    TAG, "grantUriPermission failed packageName=" + editorInfo.packageName
                            + " contentUri=" + contentUri, e
                )
            }
        }
        val inputContentInfoCompat = InputContentInfoCompat(
            contentUri,
            ClipDescription(description, arrayOf(mimeType)),
            null /* linkUrl */
        )
        InputConnectionCompat.commitContent(
            currentInputConnection, currentInputEditorInfo, inputContentInfoCompat,
            flag, null
        )
    }

    private fun validatePackageName(editorInfo: EditorInfo?): Boolean {
        if (editorInfo == null) {
            return false
        }
        val packageName = editorInfo.packageName ?: return false

        // In Android L MR-1 and prior devices, EditorInfo.packageName is not a reliable identifier
        // of the target application because:
        //   1. the system does not verify it [1]
        //   2. InputMethodManager.startInputInner() had filled EditorInfo.packageName with
        //      view.getContext().getPackageName() [2]
        // [1]: https://android.googlesource.com/platform/frameworks/base/+/a0f3ad1b5aabe04d9eb1df8bad34124b826ab641
        // [2]: https://android.googlesource.com/platform/frameworks/base/+/02df328f0cd12f2af87ca96ecf5819c8a3470dc8
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return true
        }
        val inputBinding = currentInputBinding
        if (inputBinding == null) {
            // Due to b.android.com/225029, it is possible that getCurrentInputBinding() returns
            // null even after onStartInputView() is called.
            // TODO: Come up with a way to work around this bug....
            Log.e(
                TAG, "inputBinding should not be null here. "
                        + "You are likely to be hitting b.android.com/225029"
            )
            return false
        }
        val packageUid = inputBinding.uid
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val appOpsManager = getSystemService(APP_OPS_SERVICE) as AppOpsManager
            try {
                appOpsManager.checkPackage(packageUid, packageName)
            } catch (e: Exception) {
                return false
            }
            return true
        }
        val packageManager = packageManager
        val possiblePackageNames = packageManager.getPackagesForUid(packageUid)!!
        for (possiblePackageName in possiblePackageNames) {
            if (packageName == possiblePackageName) {
                return true
            }
        }
        return false
    }

    override fun onCreate() {
        super.onCreate()

        // TODO: Avoid file I/O in the main thread.
        val imagesDir = File(filesDir, "images")
        imagesDir.mkdirs()
        mGifFile = getFileForResource(this, R.raw.animated_gif, imagesDir, "image.gif")
        mPngFile = getFileForResource(this, R.raw.dessert_android, imagesDir, "image.png")
        mWebpFile = getFileForResource(this, R.raw.animated_webp, imagesDir, "image.webp")
    }

    private fun getImages(){
        Log.i("GETIMG","getImages()")
        val projection = arrayOf<String>(MediaStore.Images.Media._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.BUCKET_ID
            )
        //val selection = null
        //val selectionArgs = null
        val selection = "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(bucket_name)
        val sortOrder ="${MediaStore.Images.Media.DATE_ADDED} DESC"

        applicationContext.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            while (cursor.moveToNext()) {

                val id = cursor.getLong(0)


                val imageUri = ContentUris
                    .withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        cursor.getInt(0) // id
                            .toLong()
                    )

                val bucketID = cursor.getInt(3)

                val bucketName = cursor.getString(2)
                Log.i("GETIMG","bucket id : $bucketID ,bucket name: $bucketName ,uri: $imageUri")
                uriList.add(imageUri)
                // Use an ID column from the projection to get
                // a URI representing the media item itself.
            }
        }
    }

    override fun onCreateInputView(): View {
        val layout = layoutInflater.inflate(R.layout.test_layout, null)
        mEditText = EditText(this)
        mGifButton = Button(this)
        mGifButton!!.text = "Test test"
        mGifButton = layout.findViewById(R.id.send_gif)
        mGifButton!!.setOnClickListener {
            doCommitContent(
                "A waving flag",
                MIME_TYPE_GIF,
                mGifFile!!
            )
        }
        mPngButton = Button(this)
        mPngButton!!.text = "Insert PNG"
        mPngButton!!.setOnClickListener {
            doCommitContent(
                "A droid logo",
                MIME_TYPE_PNG,
                mPngFile!!
            )
        }
        mWebpButton = layout.findViewById(R.id.send_webp)
        mWebpButton!!.setOnClickListener {
            doCommitContent(
                "Android N recovery animation", MIME_TYPE_WEBP, mWebpFile!!
            )
        }


        getImages()

        /*
        final LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(mEditText);
        layout.addView(mGifButton);
        layout.addView(mPngButton);
        layout.addView(mWebpButton);
        return layout;
        */

        //new KeyboardUtil(this, (KeyboardView) mkeyView.findViewById(R.id.keyboardView));

        mPngButton = layout.findViewById(R.id.send_img)
        mPngButton?.setOnClickListener(View.OnClickListener {
            doCommitContent(
                "A droid logo",
                MIME_TYPE_PNG,
                mPngFile!!
            )
        })

        // Initialize data.
        val myDataset = Datasource(applicationContext).loadAffirmations()

        val recyclerView = layout.findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.adapter = ItemAdapter(this, uriList, MemeListener { uri ->
            commitImg(uri)
            Log.i(TAG,uri.toString())
        })

        // Use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true)
        return layout
    }

    override fun onEvaluateFullscreenMode(): Boolean {
        // In full-screen mode the inserted content is likely to be hidden by the IME. Hence in this
        // sample we simply disable full-screen mode.
        return false
    }

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        mGifButton!!.isEnabled =
            mGifFile != null && isCommitContentSupported(info, MIME_TYPE_GIF)
        mPngButton!!.isEnabled =
            mPngFile != null && isCommitContentSupported(info, MIME_TYPE_PNG)
        mWebpButton!!.isEnabled =
            mWebpFile != null && isCommitContentSupported(info, MIME_TYPE_WEBP)

    }

    private fun commitImg(uri:Uri){
        Log.i(TAG,"COMMIT IMG $uri")
        doCommitContent(
            "Send img",
            MIME_TYPE_PNG,
            null,
            uri

        )
    }

    companion object {
        private const val TAG = "ImageKeyboard"
        private const val AUTHORITY = "com.example.android.commitcontent.ime.inputcontent"
        private const val MIME_TYPE_GIF = "image/gif"
        private const val MIME_TYPE_PNG = "image/png"
        private const val MIME_TYPE_WEBP = "image/webp"
        private fun getFileForResource(
            context: Context, @RawRes res: Int, outputDir: File,
            filename: String
        ): File? {
            val outputFile = File(outputDir, filename)
            val buffer = ByteArray(4096)
            var resourceReader: InputStream? = null
            return try {
                try {
                    resourceReader = context.resources.openRawResource(res)
                    var dataWriter: OutputStream? = null
                    try {
                        dataWriter = FileOutputStream(outputFile)
                        while (true) {
                            val numRead = resourceReader.read(buffer)
                            if (numRead <= 0) {
                                break
                            }
                            dataWriter.write(buffer, 0, numRead)
                        }
                        outputFile
                    } finally {
                        if (dataWriter != null) {
                            dataWriter.flush()
                            dataWriter.close()
                        }
                    }
                } finally {
                    resourceReader?.close()
                }
            } catch (e: IOException) {
                null
            }
        }
    }
}