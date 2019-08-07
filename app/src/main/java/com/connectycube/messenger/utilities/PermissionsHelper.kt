package com.connectycube.messenger.utilities

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.NonNull
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat


const val REQUEST_ATTACHMENT_IMAGE_CONTACTS = 100

class PermissionsHelper(val context: Activity) {
    val permissions: ArrayList<String> = arrayListOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)


    fun areAllImageGranted(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                permissions.all { permission ->
                    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
                }
    }

    fun requestImagePermissions() {
        requestPermissions(REQUEST_ATTACHMENT_IMAGE_CONTACTS, permissions)
    }

    fun requestPermissions(requestCode: Int, permissions: ArrayList<String>) {
        val array = arrayOfNulls<String>(permissions.size)
        permissions.toArray(array)

        ActivityCompat.requestPermissions(context, array, requestCode)
    }

}