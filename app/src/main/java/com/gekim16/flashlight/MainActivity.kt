package com.gekim16.flashlight

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.Exception
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager

private const val ON = "on"
private const val OFF = "off"

class MainActivity : AppCompatActivity() {
    private var state = OFF

    private lateinit var cameraManager : CameraManager
    private var cameraId: String? = null
    private var thread : Thread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setNoPermissionFlashListener()
    }

    private fun setNoPermissionFlashListener(){
        initFlashlight()
        flashButton.setOnClickListener {
            when(state){
                ON ->{
                    blinkFlash(-1)
                }
                OFF ->{
                    blinkFlash((seekBar.max - seekBar.progress))
                }
            }
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                if(state == ON)
                    blinkFlash(seekBar?.max ?: return)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if(seekBar == null) return
                if(state == ON)
                    blinkFlash((seekBar.max - seekBar.progress))
            }
        })
    }

    private fun initFlashlight(){
        if(!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)){
            Toast.makeText(this, "fail", Toast.LENGTH_SHORT).show()
            return
        }
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        cameraManager.cameraIdList.forEach {
            val c = cameraManager.getCameraCharacteristics(it)
            val flashAvailable = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)
            val lensFacing = c.get(CameraCharacteristics.LENS_FACING)

            if(flashAvailable != null && lensFacing == CameraCharacteristics.LENS_FACING_BACK){
                cameraId = it
            }
        }
    }

    private fun flashLightOn(){
        state = if(cameraId == null) return else ON
        cameraManager.setTorchMode(cameraId!!,true)
    }

    private fun flashLightOff(){
        state = if(cameraId == null) return else OFF
        cameraManager.setTorchMode(cameraId!!, false)
    }

    private fun blinkFlash(frequency: Int) {
        thread?.interrupt()
        thread = null

        if(frequency == seekBar.max) {
            flashLightOn()
            return
        }
        if(frequency < 0) {
            flashLightOff()
            return
        }

        val delay = (frequency * 10).toLong()
        thread = Thread(Runnable {
            try {
                var mode = OFF

                while(true){
                    mode = if(mode == OFF){
                        flashLightOn()
                        ON
                    } else{
                        flashLightOff()
                        OFF
                    }

                    Thread.sleep(delay)
                }
            }catch (e : Exception){
                e.printStackTrace()
            }
        })

        thread?.start()
    }

}