package com.connectycube.messenger.utilities.image

import android.content.Context
import android.graphics.Bitmap
import com.connectycube.messenger.R
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream

const val TEMP_CROPPED_FILE_PREFIX = "croppedImage"
const val TEMP_CROPPED_FILE_SUFFIX = ".jpg"

fun compressFileIfNeed(context: Context, path: String,
                       desiredWidth: Int = context.resources.getDimensionPixelSize(
                           R.dimen.attach_image_width
                       ),
                       desiredHeight: Int = context.resources.getDimensionPixelSize(
                           R.dimen.attach_image_height
                       )
): String {
    var tempImagePath: String? = null
    val scaledBitmap: Bitmap?

    try {
        // Decode image
        val unscaledBitmap =
            ScalingUtilities.decodeFile(
                path,
                desiredWidth,
                desiredHeight,
                ScalingUtilities.ScalingLogic.FIT
            )

        if (!(unscaledBitmap.width <= desiredWidth && unscaledBitmap.height <= desiredHeight)) {
            // Scale image
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

        val tempFile = File.createTempFile(
            TEMP_CROPPED_FILE_PREFIX,
            TEMP_CROPPED_FILE_SUFFIX,
            context.cacheDir
        )

        tempImagePath = tempFile.absolutePath
        val fos: FileOutputStream?
        try {
            fos = FileOutputStream(tempFile)
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
        Timber.e("$e")
    }

    return tempImagePath ?: path
}

fun deleteImageCacheIfNeed(pathToDelete: String) {
    val isTempFile = pathToDelete.contains(TEMP_CROPPED_FILE_PREFIX)
    Timber.d("pathToDelete= $pathToDelete, will be deleted = $isTempFile")
    if (!isTempFile) {
        return
    }
    val doomedFile = File(pathToDelete)
    try {
        val result = doomedFile.delete()
        Timber.d("deleteCache result= $result")
    } catch (e: Exception) {
        Timber.e("$e")
    }

}