package com.connectycube.messenger.utilities

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


const val REQUEST_PERMISSION_IMAGE = 100
const val REQUEST_PERMISSION_CALL = 200

class PermissionsHelper(val context: Activity) {
    private val imagePermissions: ArrayList<String> =
        arrayListOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA)

    private val callPermissions: ArrayList<String> =
        arrayListOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO).also {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) it.add(Manifest.permission.BLUETOOTH_CONNECT)
        }


    fun areAllImageGranted(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                imagePermissions.all { permission ->
                    ContextCompat.checkSelfPermission(
                        context,
                        permission
                    ) == PackageManager.PERMISSION_GRANTED
                }
    }

    fun requestImagePermissions() {
        requestPermissions(REQUEST_PERMISSION_IMAGE, imagePermissions)
    }

    private fun requestPermissions(requestCode: Int, permissions: ArrayList<String>) {
        val array = arrayOfNulls<String>(permissions.size)
        permissions.toArray(array)

        ActivityCompat.requestPermissions(context, array, requestCode)
    }

    fun areCallPermissionsGranted(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                callPermissions.all { permission ->
                    ContextCompat.checkSelfPermission(
                        context,
                        permission
                    ) == PackageManager.PERMISSION_GRANTED
                }
    }

    fun requestCallPermissions() {
        requestPermissions(REQUEST_PERMISSION_CALL, callPermissions)
    }
}