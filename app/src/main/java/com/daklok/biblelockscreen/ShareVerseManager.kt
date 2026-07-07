package com.daklok.biblelockscreen

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object ShareVerseManager {


    sealed class Result {
        object Success : Result()
        data class Failure(val message: String) : Result()
    }


    @RequiresApi(Build.VERSION_CODES.P)
    fun generateVerseBitmap(
        context: Context,
        imageUri: Uri?,
        verseText: String,
        verseRef: String,
        textSizeMultiplier: Float,
        textWidthMultiplier: Float,
        verticalOffset: Float,
        textColor: Int,
        textAlpha: Float,
        isBold: Boolean,
        useShadow: Boolean,
        fontFamilyStr: String,
        bgBlur: Float,
        bgDarkness: Float
    ): Bitmap? {
        return if (imageUri != null) {
            WallpaperUtils.createBitmapWithText(
                context = context,
                imageUri = imageUri,
                verse = verseText,
                ref = verseRef,
                textSizeMultiplier = textSizeMultiplier,
                textWidthMultiplier = textWidthMultiplier,
                verticalOffset = verticalOffset,
                textColorInt = textColor,
                textAlpha = textAlpha,
                isBold = isBold,
                useShadow = useShadow,
                fontFamilyStr = fontFamilyStr,
                bgBlurRadius = bgBlur,
                bgDarkness = bgDarkness
            )
        } else {
            // Fallback: gradient background when no photo is set
            WallpaperUtils.createBitmapWithGradient(
                context = context,
                verse = verseText,
                ref = verseRef,
                textSizeMultiplier = textSizeMultiplier,
                textWidthMultiplier = textWidthMultiplier,
                verticalOffset = verticalOffset,
                textColorInt = textColor,
                textAlpha = textAlpha,
                isBold = isBold,
                useShadow = useShadow,
                fontFamilyStr = fontFamilyStr,
                bgDarkness = 0f  // gradient is already dark enough
            )
        }
    }

    /**
     * Shares the verse bitmap via Android's share sheet (ACTION_SEND).
     * The bitmap is written to [Context.getCacheDir] as a temporary JPEG
     * and exposed to other apps via FileProvider. No permissions needed.
     */
    fun shareBitmap(context: Context, bitmap: Bitmap): Result {
        return try {
            val file = writeBitmapToCache(context, bitmap)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "Share verse image")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            Result.Success
        } catch (e: Exception) {
            e.printStackTrace()
            Result.Failure(e.message ?: "Unknown error")
        }
    }

    /**
     * Saves the verse bitmap to the device's gallery (Pictures/BibleLockScreen/).
     *
     * On API 29+ (scoped storage): uses MediaStore with no permissions needed.
     * On API 24-28: uses MediaStore but requires WRITE_EXTERNAL_STORAGE
     *   permission, which the caller must have already requested.
     */
    fun saveToGallery(context: Context, bitmap: Bitmap): Result {
        return try {
            val filename = "bible_verse_${System.currentTimeMillis()}.jpg"

            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Scoped storage (API 29+): save to Pictures/BibleLockScreen/
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/BibleLockScreen")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                } else {
                    // Legacy storage (API 24-28): save to public Pictures directory
                    @Suppress("DEPRECATION")
                    put(MediaStore.Images.Media.DATA,
                        File(getLegacyPicturesDir(), filename).absolutePath)
                }
            }

            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
            ) ?: return Result.Failure("Could not create gallery entry")

            context.contentResolver.openOutputStream(uri)?.use { os: OutputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, os)
            } ?: return Result.Failure("Could not open output stream")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
            }

            Result.Success
        } catch (e: Exception) {
            e.printStackTrace()
            Result.Failure(e.message ?: "Unknown error")
        }
    }

    // -- private helpers --

    private fun writeBitmapToCache(context: Context, bitmap: Bitmap): File {
        val file = File(context.cacheDir, "shared_verse.jpg")
        FileOutputStream(file).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos)
        }
        return file
    }

    private fun getLegacyPicturesDir(): File {
        @Suppress("DEPRECATION")
        val pictures = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES
        )
        val dir = File(pictures, "BibleLockScreen")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }
}
