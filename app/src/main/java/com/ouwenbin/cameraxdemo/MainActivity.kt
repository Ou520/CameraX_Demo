package com.ouwenbin.cameraxdemo

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment.*
import android.os.Handler
import android.os.Message
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.ouwenbin.cameraxdemo.utils.LuminosityAnalyzer
import com.ouwenbin.cameraxdemo.utils.Util
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


private const val PERMISSIONS_REQUEST_CODE = 10//请求码

//动态请求权限的数组
private val PERMISSIONS_REQUIRED = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )//请求权限的数组，可以在数组中添加你需要动态获取的权限

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    /*-----------------View的绑定----------------*/
    private lateinit var viewFinder: PreviewView
    private lateinit var camera_switch:ImageButton
    private lateinit var camera_capture:ImageButton
    private lateinit var photo_view :ImageButton

    /*----------------------- 相机的初始化和绑定 ------------------------------*/
    private var preview: Preview? = null//定义Preview对象
    private var imageCapture: ImageCapture? = null//片拍摄用例对象
    private var imageAnalyzer: ImageAnalysis? = null//分析图片
    private lateinit var cameraExecutor: ExecutorService//阻塞相机操作是使用这个执行器执行
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK//设置相机的前后置
    private var displayId: Int = -1    //屏幕Id

    /*------------------- 拍照 -------------------------*/
    private lateinit var mHandler: Handler//定义Handler对象
    private var PhotoPath: String = ""
    var photoFile: File? = null



    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initView()

        //请求动态获取权限
        if (!hasPermissions(this)) {
            // 请求camera权限
            requestPermissions(PERMISSIONS_REQUIRED, PERMISSIONS_REQUEST_CODE)
        } else {

            //权限获取成功后
//            Toast.makeText(this, "权限获取成功", Toast.LENGTH_LONG).show()

            //启动相机
            startCamera()
        }

        initListener()

    }



    private fun initView() {
        viewFinder = findViewById(R.id.pv_main)
        camera_switch = findViewById(R.id.camera_switch_button)
        camera_capture = findViewById(R.id.camera_capture_button)
        photo_view = findViewById(R.id.photo_view_button)


        //创建线程池来执行Camera 的操作
        cameraExecutor = Executors.newSingleThreadExecutor()

        //检查PhotoView的状态的方法
        checkPhotoView()


    }



    private fun initListener() {

        //相机切换按钮
        camera_switch.setOnClickListener(View.OnClickListener {

            lensFacing = if (CameraSelector.LENS_FACING_FRONT == lensFacing) {
                CameraSelector.LENS_FACING_BACK
            } else {
                CameraSelector.LENS_FACING_FRONT
            }

            //重新启动相机
            bindCameraUseCases()

        })


        //拍照按钮的监听
        camera_capture.setOnClickListener(View.OnClickListener {

            //拍照的方法
            TakeAPicture()

            //初始化Handler对象，并进行消息处理
            mHandler = Handler{
                when (it.what) {
                    1 -> { Glide.with(this).load(PhotoPath).apply(RequestOptions.circleCropTransform()).into(photo_view) }
                }
                false }
        })


        //图库按钮的点击事件
        photo_view.setOnClickListener(View.OnClickListener {

            if (PhotoPath !=""){

                //让intent携带数据跳转到ImgShow
                val intent = Intent(this, PhotoShowActivity::class.java)
                intent.putExtra("urlKey", PhotoPath)
                startActivity(intent)

                //图片路径
//                Toast.makeText(this,"$PhotoPath",Toast.LENGTH_SHORT).show()
            }
        })

    }


    //检查PhotoView的状态的方法
    private fun checkPhotoView() {
        val intent = intent
        val shanchu = intent.getStringExtra("shanchu") //图片地址
        if (shanchu == "shanchu"){ Glide.with(this).load(R.drawable.ic_photo).into(photo_view) }
    }


    //启动相机
    private fun startCamera() {

        //打开相机
        viewFinder.post {
            // 跟踪这个视图所附加的显示
            displayId = viewFinder.display.displayId

            //初始化CameraX，并准备绑定相机用例
            setUpCamera()
        }

    }


    private var cameraProvider: ProcessCameraProvider? = null
    /** 初始化CameraX，并准备绑定相机用例  */
    private fun setUpCamera() {

        //请求CameraProvider
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        //检查 CameraProvider 可用性
        cameraProviderFuture.addListener(Runnable {

            //获取CameraProvider对象，并赋值我全局变量
            cameraProvider = cameraProviderFuture.get()

            // 创建和绑定相机用例
            bindCameraUseCases()

        }, ContextCompat.getMainExecutor(this))
    }


    // 创建和绑定相机用例(CameraX 的API的使用都在这个方法里)
    private fun bindCameraUseCases() {

        // 获取用于设置摄像头为全屏分辨率的屏幕指标
        val metrics = DisplayMetrics().also { viewFinder.display.getRealMetrics(it) }
        //获取预览框长宽比
        val screenAspectRatio = Util().aspectRatio(metrics.widthPixels, metrics.heightPixels)
        //获取屏幕的旋转方向
        val rotation = viewFinder.display.rotation


        //把全局CameraProvider赋值给局部对象
        val cameraProvider = cameraProvider ?: throw IllegalStateException("相机初始化失败")



        //创建Preview对象
        preview = Preview.Builder()
            .setTargetAspectRatio(screenAspectRatio)//设置预览的宽高比（或者分辨率）
            .setTargetRotation(rotation)//设定初始旋转方向（横竖）
            .build()

        //指定相机是 前置 还是 后置
        /*
        * 前置相机：CameraSelector.LENS_FACING_FRONT
        * 后置相机：CameraSelector.LENS_FACING_BACK
        * */
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()


        /*
        * 设置分析图片用例
        *
        * 作用：提供可供 CPU 访问的图片来执行图片处理、计算机视觉或机器学习推断
        * */
        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(screenAspectRatio)//指定分辨率
            .setTargetRotation(rotation)
             /*
             * 阻塞模式：ImageAnalysis.STRATEGY_BLOCK_PRODUCER  （在此模式下，执行器会依序从相应相机接收帧；这意味着，如果 analyze() 方法所用的时间超过单帧在当前帧速率下的延迟时间，所接收的帧便可能不再是最新的帧，因为在该方法返回之前，新帧会被阻止进入流水线）
             * 非阻塞模式： ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST （在此模式下，执行程序在调用 analyze() 方法时会从相机接收最新的可用帧。如果此方法所用的时间超过单帧在当前帧速率下的延迟时间，它可能会跳过某些帧，以便 analyze() 在下一次接收数据时获取相机流水线中的最新可用帧）
             * */
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)//图片分析模式
            .build()
            .also {
                it.setAnalyzer(cameraExecutor, LuminosityAnalyzer { luma ->

                    //分析图片用例返回的结果
//                    Log.d("分析图片用例返回的结果：", "$luma")
                })
            }

        /*
        * 设置图片拍摄用例
        * ImageCapture的介绍：https://developer.android.google.cn/reference/androidx/camera/core/ImageCapture?hl=zh_cn#CAPTURE_MODE_MINIMIZE_LATENCY
        * */
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)//设置拍照模式
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)//设置旋转
            .build()



        //必须在重新绑定用例之前解除绑定
        cameraProvider.unbindAll()

        try {
            //将所选相机和任意用例绑定到生命周期 将 Preview 连接到 PreviewView
           var camera: Camera? = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, imageAnalyzer)


            //将取景器的视图与预览用例进行绑定
            preview?.setSurfaceProvider(viewFinder.surfaceProvider)

        } catch (exc: Exception) {
            Log.e("TAG", "相机启动失败", exc)
        }

    }



    //拍照的方法
    private fun TakeAPicture() {

        // 获取可修改图像捕获用例的稳定参考
        imageCapture?.let { imageCapture ->

            // 创建文件对象来保存图像
            val photoFile: File? = try {
                createImageFile()
            } catch (ex: IOException) {

                Log.d("文件创建失败","${ex}")
                null
            }

            // 设置图像捕获元数据
            val metadata = ImageCapture.Metadata().apply {

                //使用前置摄像头时的镜像
                isReversedHorizontal = lensFacing == CameraSelector.LENS_FACING_FRONT
            }

            // 创建输出选项对象（file：输出的文件；metadata：照片的元数据）
            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile!!)
                .setMetadata(metadata)
                .build()

            // 设置图像捕捉监听器，在照片拍摄完成后触发
            imageCapture.takePicture(outputOptions, cameraExecutor, object : ImageCapture.OnImageSavedCallback {


                //拍照成功的回调（保存照片）
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {

                    //照片的存储路径
                    val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
                    val savedUrl = savedUri.toString()

                    //将照片添加到图库
                    galleryAddPic()

                    //使用Handler发送消息
                    val msg: Message = Message.obtain()
                    msg.what = 1
                    mHandler.sendMessage(msg)

                    Log.d(TAG, "拍照成功返回的路径: $savedUri")

                }

                //拍照失败的回调
                override fun onError(exc: ImageCaptureException) {
                    Log.e("TAG", "拍照失败: ${exc.message}", exc)
                }
            })
        }

    }


    //创建文件对象
    @SuppressLint("SimpleDateFormat")
    @Throws(IOException::class)
    private fun createImageFile(): File {

        //文件名
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())

        //文件路径（SD卡路径）
        val storageDir =getExternalStorageDirectory()

        return File.createTempFile(
                "${timeStamp}_",
                ".jpg",
                storageDir
        ).apply {

            //返回文件的路径
            PhotoPath = absolutePath

        }
    }


    //将照片添加到图库
    private fun galleryAddPic() {
        Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).also { mediaScanIntent ->
            photoFile = File(PhotoPath)
            mediaScanIntent.data = Uri.fromFile(photoFile)
            sendBroadcast(mediaScanIntent)
        }
    }




    //授权结果处理函数
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (PackageManager.PERMISSION_GRANTED == grantResults.firstOrNull()) {

                //用户点击权限给予按钮
//                Toast.makeText(this, "获取到权限", Toast.LENGTH_LONG).show()

                //启动相机
                startCamera()


            } else {
                //用户点击拒绝按钮
                Toast.makeText(this, "请赋予权限，否则无法正常使用本软件", Toast.LENGTH_LONG).show()

                /* 可以跳转到权限设置界面，也可以关闭软件 */
            }
        }
    }


    //Activity生命周期的销毁回调
    override fun onDestroy() {
        super.onDestroy()
        //关闭CameraExecutor
        cameraExecutor.shutdown()

    }


    //共享方法和常量
    companion object {

        //时间戳转换公式
        private const val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
        //照片的输出格式
        private const val PHOTO_EXTENSION = ".jpg"

        private const val TAG = "MyCameraXDemo"//标识

        /**  用于检查应用程序所需的所有权限是否都被授予的方法 **/
        fun hasPermissions(context: Context) = PERMISSIONS_REQUIRED.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

    }
}