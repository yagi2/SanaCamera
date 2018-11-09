package com.yagi2.sanacamera

import android.annotation.SuppressLint
import android.content.Context
import android.databinding.DataBindingUtil
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.support.annotation.RequiresApi
import android.support.v7.app.AppCompatActivity
import android.util.Size
import android.view.Surface
import android.view.TextureView
import com.yagi2.sanacamera.databinding.ActivityMainBinding
import kotlin.math.min

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var cameraDevice: CameraDevice? = null
    private var previewRequestBuilder: CaptureRequest.Builder? = null
    private var imageReader: ImageReader? = null

    private var backgroundThread: HandlerThread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        binding.surface.surfaceTextureListener = surfaceTextureListener

        startBackgroundThread()
    }

    @SuppressLint("MissingPermission")
    private fun openCamera() {
        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val camerId = manager.cameraIdList[0]
        manager.openCamera(camerId, cameraDeviceStateCallback, null)
    }

    private fun createCameraPreviewSession() {
        try {
            val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            manager.cameraIdList.forEach {
                val characteristics = manager.getCameraCharacteristics(it)
                val cameraDirection = characteristics.get(CameraCharacteristics.LENS_FACING)

                if (cameraDirection != null && cameraDirection == CameraCharacteristics.LENS_FACING_FRONT) {
                    return@forEach
                }

                val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) ?: return@forEach

                val previewSize = map.getOutputSizes(SurfaceTexture::class.java).toList()
                    .chooseOptimalSize(Size(binding.root.width * 2, binding.root.height * 2))

                binding.surface.ratio = previewSize
            }

            val texture = binding.surface.surfaceTexture.apply {
                setDefaultBufferSize(binding.surface.ratio.width, binding.surface.ratio.height)
            }

            val surface = Surface(texture)

            previewRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            previewRequestBuilder?.addTarget(surface)

            cameraDevice?.createCaptureSession(
                listOf(surface, imageReader?.surface),
                cameraCaptureSessionStateCallback,
                null
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground").also { it.start() }
    }

    private fun List<Size>.chooseOptimalSize(preferredSize: Size): Size {
        val shortSideLength = min(preferredSize.width, preferredSize.height)
        val bigEnough = ArrayList<Size>()
        val notBigEnough = ArrayList<Size>()

        this.forEach {
            if (it == preferredSize) return it

            (if (it.width >= shortSideLength && it.height >= shortSideLength) bigEnough
            else notBigEnough)
                .add(it)
        }

        return when {
            bigEnough.isNotEmpty() -> bigEnough.minBy { it.width * it.height } ?: this[0]
            notBigEnough.isNotEmpty() -> notBigEnough.maxBy { it.width * it.height } ?: this[0]
            else -> this[0]
        }
    }

    private val cameraDeviceStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(cameraDevice: CameraDevice) {
            this@MainActivity.cameraDevice = cameraDevice
            createCameraPreviewSession()
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {
            cameraDevice.close()
            this@MainActivity.cameraDevice = null
        }

        override fun onError(cameraDevice: CameraDevice, error: Int) {
            onDisconnected(cameraDevice)
        }
    }

    private val cameraCaptureSessionStateCallback = object : CameraCaptureSession.StateCallback() {

        override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
            try {
                previewRequestBuilder?.set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                )

                cameraCaptureSession.setRepeatingRequest(
                    requireNotNull(previewRequestBuilder?.build()), null, Handler(backgroundThread?.looper)
                )
            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }
        }

        override fun onConfigureFailed(session: CameraCaptureSession) {}
    }


    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {

        @RequiresApi(Build.VERSION_CODES.M)
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            imageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 2)
            openCamera()
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture) = false
    }
}