package com.connectycube.messenger.utilities.image

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream


fun compressFile(context: Context, path: String, desiredWidth: Int = 400, desiredHeight: Int = 800): String {
    var strMyImagePath: String? = null
    var scaledBitmap: Bitmap? = null

    try {
        // Part 1: Decode image
        val unscaledBitmap =
            ScalingUtilities.decodeFile(
                path,
                desiredWidth,
                desiredHeight,
                ScalingUtilities.ScalingLogic.FIT
            )

        if (!(unscaledBitmap.getWidth() <= desiredWidth && unscaledBitmap.height <= desiredHeight)) {
            // Part 2: Scale image
            scaledBitmap = ScalingUtilities.createScaledBitmap(
                unscaledBitmap,
                desiredWidth,
                desiredHeight,
                ScalingUtilities.ScalingLogic.FIT
            )
        } else {
            unscaledBitmap.recycle()
            return path
        }

        // Store to tmp file

        val destinationFileName = "decodedImage.jpg"
        val destination = Uri.fromFile(File(context.cacheDir, destinationFileName))
        val f = File(destination.path)

        strMyImagePath = f.absolutePath
        var fos: FileOutputStream? = null
        try {
            fos = FileOutputStream(f)
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 75, fos)
            fos.flush()
            fos.close()
        } catch (e: FileNotFoundException) {

            e.printStackTrace()
        } catch (e: Exception) {

            e.printStackTrace()
        }

        scaledBitmap.recycle()
    } catch (e: Throwable) {
    }

    return strMyImagePath ?: path

}