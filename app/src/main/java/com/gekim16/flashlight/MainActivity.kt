package com.gekim16.flashlight

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.gekim16.flashlight.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var cameraManager: CameraManager
    private lateinit var binding: ActivityMainBinding
    private var cameraId: String? = null
    private val viewModel = MainViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.viewModel = viewModel

        initFlashlight()
        setNoPermissionFlashListener()
    }

    private fun setNoPermissionFlashListener() {
        viewModel.state
            .observe(this, Observer {
                if(it) {
                    flashLightOn()
                } else {
                    flashLightOff()
                }
            })
    }

    private fun initFlashlight() {
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            Toast.makeText(this, "fail", Toast.LENGTH_SHORT).show()
            return
        }
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        cameraManager.cameraIdList.forEach {
            val c = cameraManager.getCameraCharacteristics(it)
            val flashAvailable = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)
            val lensFacing = c.get(CameraCharacteristics.LENS_FACING)

            if (flashAvailable != null && lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                cameraId = it
            }
        }
    }

    private fun flashLightOn() {
        cameraId?.let {
            cameraManager.setTorchMode(it, true)
        }
    }

    private fun flashLightOff() {
        cameraId?.let {
            cameraManager.setTorchMode(it, false)
        }
    }
}