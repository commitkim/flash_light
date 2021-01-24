package com.gekim16.flashlight

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit

private const val ON = "on"
private const val OFF = "off"

class MainActivity : AppCompatActivity() {
    private var state = OFF
    private var isFlickering = false

    private lateinit var cameraManager: CameraManager
    private var cameraId: String? = null
    private val seekBarMax: Int by lazy { seekBar.max }

    private var thread: Thread? = null


    private val handler: Handler = Handler(Looper.getMainLooper())
    private val intervalTask: Runnable by lazy { object : Runnable {
        override fun run() {
            if (!isFlickering) return

            flicker()
            handler.postDelayed(this, getInterval())
        }
    } }

    private var timer: Timer? = null

    private var job : Job? = null

    private var observable : Disposable? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setNoPermissionFlashListener()
    }

    private fun setNoPermissionFlashListener() {
        initFlashlight()
        flashButton.setOnClickListener {

            if(isFlickering) {
                stopFlickering()
            }
            else{
                startFlickeringWithRxJava()
            }
            isFlickering = !isFlickering
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let{
                    if(isFlickering)
                        startFlickeringWithRxJava()
                }
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

    private fun getInterval() = ((seekBarMax - seekBar.progress) * 10).toLong()

    private fun flashLightOn() {
        cameraId?.let {
            state = ON
            cameraManager.setTorchMode(it, true)
        }
    }

    private fun flashLightOff() {
        cameraId?.let {
            state = OFF
            cameraManager.setTorchMode(it, false)
        }
    }

    private fun flicker() {
        if (state == OFF) {
            flashLightOn()
        } else {
            flashLightOff()
        }
    }

    // isContinue 등의 변수로 종료시켰을 때 확실히 꺼지지 않으면 두개의 깜빡이는 메소드가 동작되는 문제 발생으로 확실히 종료시켜주는 메소드를 추가하는 방법 선택
    private fun stopFlickering(){
        thread?.interrupt()
        job?.cancel()
        timer?.cancel()
        observable?.dispose()

        flashLightOff()
    }

    /**
     * Use Thread
     */
    private fun startFlickeringWithThread() {
        stopFlickering()

        if (seekBar.progress == 0) {
            flashLightOn()
            return
        }

        thread = Thread(Runnable {
            try {
                while(isFlickering){
                    flicker()
                    Thread.sleep(getInterval())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        })

        thread?.start()
    }


    /**
     *  Use Handler
     */
    private fun startFlickeringWithHandler() {
        if (seekBar.progress == 0) {
            flashLightOn()
            return
        }

        handler.post(intervalTask)
    }


    /**
     *  Use Coroutine
     */
    private fun startFlickeringWithCoroutine(){
        stopFlickering()

        if (seekBar.progress == 0) {
            flashLightOn()
            return
        }

        job = CoroutineScope(Default).launch{
            while(isFlickering){
                flicker()
                delay(getInterval())
            }
        }
    }

    /**
     *  Use Timer
     */
    private fun startFlickeringWithTimer(){
        stopFlickering()

        if (seekBar.progress == 0) {
            flashLightOn()
            return
        }

        val interval = getInterval()
        timer = Timer()

        timer?.schedule(object : TimerTask() {
            override fun run() {
                flicker()
            }
        }, 0, if(interval <= 0 ) 1 else interval) // 0 이하의 딜레이일땐 에러가 발생
    }

    /**
     *  Use RxJava
     */
    private fun startFlickeringWithRxJava(){
        stopFlickering()

        if (seekBar.progress == 0) {
            flashLightOn()
            return
        }

        observable = io.reactivex.rxjava3.core.Observable.timer(getInterval(), TimeUnit.MILLISECONDS)  // 몇 ms 뒤에 실핼할 것인지 결정
            .repeat() //반복
            .subscribeOn(Schedulers.computation())  // subscribe 에 사용할 쓰레드 지정
            .observeOn(Schedulers.newThread()) // Observable 이 다음 처리를 진행할 때 사용할 쓰레드를 지정
            .subscribe {
                if(isFlickering)
                    flicker()
            }
    }
}