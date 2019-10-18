package com.connectycube.messenger.utilities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.widget.Toast
import androidx.exifinterface.media.ExifInterface
import com.connectycube.messenger.R
import com.yalantis.ucrop.UCrop
import com.zhihu.matisse.Matisse
import com.zhihu.matisse.MimeType
import com.zhihu.matisse.internal.entity.CaptureStrategy
import com.zhihu.matisse.listener.OnCheckedListener
import timber.log.Timber
import java.io.File
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.FileInputStream
import java.io.FileOutputStream


const val REQUEST_CODE_CHOOSE = 23

fun requestImage(activity: Activity) {
    Matisse.from(activity)
        .choose(MimeType.ofImage(), false)
        .showSingleMediaType(true)
        .countable(false)
        .capture(true)
        .captureStrategy(
            CaptureStrategy(true, "com.connectycube.messenger.fileprovider")
        )
        .maxSelectable(1)
//                .addFilter(GifSizeFilter(320, 320, 5 * Filter.K * Filter.K))
        .gridExpectedSize(
            activity.resources.getDimensionPixelSize(R.dimen.grid_expected_size)
        )
        .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        .thumbnailScale(0.85f)
        .imageEngine(Glide4Engine())
        .setOnSelectedListener { uriList, pathList ->
            // DO SOMETHING IMMEDIATELY HERE
            Timber.d("onSelected= pathList=$pathList")
        }
        .originalEnable(true)
        .maxOriginalSize(10)
//                .autoHideToolbarOnSingleTap(true)
        .setOnCheckedListener(OnCheckedListener { isChecked ->
            // DO SOMETHING IMMEDIATELY HERE
            Timber.d("isChecked= isChecked=$isChecked")
        })
        .forResult(REQUEST_CODE_CHOOSE)
}

fun cropImage(activity: Activity, path: String) {
    val ratioX = 3f
    val ratioY = 4f
    val maxWidth = 600
    val maxHeight = 800
    val options = UCrop.Options()
    options.setCircleDimmedLayer(true)
    val sourceUri = Uri.fromFile(File(path))
    val destinationFileName = "croppedImage.jpg"
    val destination = Uri.fromFile(File(activity.cacheDir, destinationFileName))
    UCrop.of(sourceUri, destination)
        .withOptions(options)
        .withAspectRatio(ratioX, ratioY)
        .start(activity)
}

fun handleCropError(ctx: Context, result: Intent) {
    val cropError = UCrop.getError(result)
    if (cropError != null) {
        Timber.d("handleCropError: $cropError")
        Toast.makeText(ctx, cropError.message, Toast.LENGTH_LONG).show()
    } else {
        Timber.d("handleCropError: unexpected error")
    }
}

fun getImageSize(path: String): Size {
    val exif = ExifInterface(path)
    val width = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
    val height = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)
    return Size(width, height)
}

fun compressImage(file: File): File {
    try {

        // BitmapFactory options to downsize the image
        val o = BitmapFactory.Options()
        o.inJustDecodeBounds = true
        o.inSampleSize = 6
        // factor of downsizing the image

        var inputStream = FileInputStream(file)
        BitmapFactory.decodeStream(inputStream, null, o)
        inputStream.close()

        // The new size we want to scale to
        val REQUIRED_SIZE = 75

        // Find the correct scale value. It should be the power of 2.
        var scale = 1
        while (o.outWidth / scale / 2 >= REQUIRED_SIZE && o.outHeight / scale / 2 >= REQUIRED_SIZE) {
            scale *= 2
        }

        val o2 = BitmapFactory.Options()
        o2.inSampleSize = scale
        inputStream = FileInputStream(file)

        val selectedBitmap = BitmapFactory.decodeStream(inputStream, null, o2)
        inputStream.close()

        //create new file instead of the original image file
        val newFile = File(file.path, "tmp.png")
        val outputStream = FileOutputStream(newFile)

        selectedBitmap?.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)

        return newFile
    } catch (e: Exception) {
        return file
    }
}