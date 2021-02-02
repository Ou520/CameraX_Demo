
# CameraX 的使用简介

### 介绍

CameraX 是一个 `Jetpack` 支持库，旨在帮助您简化相机应用的开发工作。它提供一致且易于使用的 API Surface，适用于大多数 Android 设备，并可向后兼容至 Android 5.0（API 级别 21）。

虽然它利用的是 camera2 的功能，但使用的是更为简单且基于用例的方法，该方法具有生命周期感知能力。它还解决了设备兼容性问题，因此您无需在代码库中添加设备专属代码。这些功能减少了将相机功能添加到应用时需要编写的代码量。

.


### CameraX 的架构

.

![在这里插入图片描述](https://img-blog.csdnimg.cn/img_convert/1572a5de3f72c6b87f39907eba5e6568.png#pic_center)


.

### CameraX 的优点

>- **易用性**
> CameraX 引入了多个用例，使您可以专注于需要完成的任务，而无需花时间处理不同设备之间的细微差别；如：预览用例，图片分析用例，图片拍摄用例等。
>
>- **新的相机体验**
> CameraX 有一个名为 `Extensions` 的可选插件，开发者可以通过扩展的形式使用和原生摄像头应用同样的功能（如：人像、夜间模式、HDR、滤镜、美颜）
>
>- **生命周期管理**
> CameraX 和 `Lifecycle` 结合在一起，方便开发者管理生命周期。且相比较 `camera2` 减少了大量样板代码的使用
>
>- **确保各设备间的一致性**
> Google 自己还打造了 CameraX 自动化测试实验室，对摄像头功能进行深度测试，确保能覆盖到更加广泛的设备。相当于 Google 帮我们把设备兼容测试工作给做了。
>
> **补充：**
> 
> 对于开发者来说，简单易用的 API、更少的模版代码、更强的兼容性，意味着更高的开发和测试效率。而丰富的扩展性则意味着开发者可以为用户们带来更多基于摄像头的光影体验。


.

### CameraX 使用要求

>**CameraX 具有以下最低版本要求：**
>
>- **Android API 级别 >= 21**
>
>- **Android 架构组件 1.1.1**
>
>- **能够感知生命周期的 `Activity`，请使用 `FragmentActivity` 或 `AppCompatActivity`**
>
>- **Android Studio 3.6以上版本**

.

### CameraX 的简单使用


### 添加依赖

打开项目的 `build.gradle` 文件并添加 `google()` 代码库，如下所示：


```java
allprojects {
    repositories {
        google()
        jcenter()
    }
}
```

将以下内容添加到 `Android` 代码块的末尾：

CameraX需要Java 8中的某些方法，因此我们需要相应地设置编译选项。在该android块的末尾，紧接着buildTypes，添加以下内容：


```java
compileOptions {
    sourceCompatibility JavaVersion.VERSION_1_8
    targetCompatibility JavaVersion.VERSION_1_8
}

kotlinOptions {
    jvmTarget = "1.8"
}
```

在应用每模块的 `build.gradle` 文件中添加CameraX的依赖：

```java
dependencies {

  // CameraX core library using the camera2 implementation
  def camerax_version = "1.0.0-rc01"
  // The following line is optional, as the core library is included indirectly by camera-camera2
  implementation "androidx.camera:camera-core:${camerax_version}"
  implementation "androidx.camera:camera-camera2:${camerax_version}"
  // If you want to additionally use the CameraX Lifecycle library
  implementation "androidx.camera:camera-lifecycle:${camerax_version}"
  // If you want to additionally use the CameraX View class
  implementation "androidx.camera:camera-view:1.0.0-alpha20"
  // If you want to additionally use the CameraX Extensions library
  implementation "androidx.camera:camera-extensions:1.0.0-alpha20"

}
```


首先在应用的 `manifest` 添加 Camera 权限：

```java
<!-- 启用即时应用支持 -->
<dist:module dist:instant="true" /> <!-- Declare features -->
<uses-feature android:name="android.hardware.camera" /> <!-- Declare permissions -->
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```



### 开始使用CameraX


**1. 首先动态请求 `Camera` 的权限**

检查用户是否赋予权限

```java
    //请求动态获取权限
    if (!hasPermissions(this)) {
        // 请求camera权限
        requestPermissions(PERMISSIONS_REQUIRED, PERMISSIONS_REQUEST_CODE)
    } else {

        //权限获取成功后
        Toast.makeText(this, "权限获取成功", Toast.LENGTH_LONG).show()

        //启动相机
        startCamera()
    }
```


对用户授权结果进行相关处理

```java
//授权结果处理函数
override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)

    if (requestCode == PERMISSIONS_REQUEST_CODE) {
        if (PackageManager.PERMISSION_GRANTED == grantResults.firstOrNull()) {

            //用户点击权限给予按钮
            Toast.makeText(this, "获取到权限", Toast.LENGTH_LONG).show()

            //启动相机
            startCamera()

        } else {
            //用户点击拒绝按钮
            Toast.makeText(this, "请赋予权限，否则无法正常使用本软件", Toast.LENGTH_LONG).show()
			
			/* 可以跳转到权限设置界面，也可以关闭软件 */
        }
    }
}
```

**2. 使用 `PreviewView` 实现相机的预览**

> **如需使用 `PreviewView` 实现 `CameraX` 预览，请按以下步骤操作（稍后将对这些步骤进行说明）：**
>
> 1. 配置 `CameraXConfig.Provider`(可选)
>
> 2. 将 `PreviewView` 添加到布局
>
> 3. 请求 `ProcessCameraProvider`
>
> 4. 在创建 `View` 时，请检查 `ProcessCameraProvider`
>
> 5. 选择相机并绑定生命周期和用例



**1. 搭建预览视图的布局，代码如下：**

```c
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".MainActivity">

    <androidx.camera.view.PreviewView
        android:id="@+id/pv_main"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        tools:layout_editor_absoluteX="0dp"
        tools:layout_editor_absoluteY="-27dp" />

    <ImageButton
        android:id="@+id/camera_switch_button"
        android:layout_width="64dp"
        android:layout_height="64dp"
        android:layout_marginBottom="92dp"
        android:layout_marginStart="32dp"
        android:padding="40dp"
        android:scaleType="fitCenter"
        android:background="@drawable/ic_switch"
        app:layout_constraintLeft_toLeftOf="parent"
        app:layout_constraintBottom_toBottomOf="parent"
        android:contentDescription="Switch camera" />

    <ImageButton
        android:id="@+id/camera_capture_button"
        android:layout_width="92dp"
        android:layout_height="92dp"
        android:layout_marginBottom="80dp"
        android:scaleType="fitCenter"
        android:background="@drawable/ic_shutter"
        app:layout_constraintLeft_toLeftOf="parent"
        app:layout_constraintRight_toRightOf="parent"
        app:layout_constraintBottom_toBottomOf="parent"
        android:contentDescription="Capture" />

    <ImageButton
        android:id="@+id/photo_view_button"
        android:layout_width="64dp"
        android:layout_height="64dp"
        android:layout_marginBottom="92dp"
        android:layout_marginEnd="32dp"
        android:padding="10dp"
        android:scaleType="fitCenter"
        android:background="@drawable/ic_outer_circle"
        app:srcCompat="@drawable/ic_photo"
        app:layout_constraintRight_toRightOf="parent"
        app:layout_constraintBottom_toBottomOf="parent"
        android:contentDescription="Gallery" />
</androidx.constraintlayout.widget.ConstraintLayout>
```


**2. 创建和绑定相机用例**


**2.1 请求 CameraProvider,代码如下：**

```java
class MainActivity : AppCompatActivity() {
    private lateinit var cameraProviderFuture : ListenableFuture<ProcessCameraProvider>
    override fun onCreate(savedInstanceState: Bundle?) {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
    }
}
```

**2.2 检查 CameraProvider 可用性**

请求 `CameraProvider` 后，请验证它能否在视图创建后成功初始化。代码如下：

```java
//检查 CameraProvider 可用性
cameraProviderFuture.addListener(Runnable {

    //获取CameraProvider对象，并赋值我全局变量
    cameraProvider = cameraProviderFuture.get()

    // 创建和绑定相机用例
    bindCameraUseCases()

}, ContextCompat.getMainExecutor(this))
```

2.3 创建 `Preview`对象

```java
//创建Preview对象
preview = Preview.Builder()
    .setTargetAspectRatio(screenAspectRatio)//设置预览的宽高比（或者分辨率）
    .setTargetRotation(rotation)//设定初始旋转方向（横竖）
    .build()
```

**2.4 指定所需的相机 LensFacing 选项**

```java
//指定相机是 前置 还是 后置
/*
* 前置相机：CameraSelector.LENS_FACING_FRONT
* 后置相机：CameraSelector.LENS_FACING_BACK
* */
val cameraSelector = CameraSelector.Builder()
    .requireLensFacing(lensFacing)
    .build()
```

**2.5 将所选相机和任意用例绑定到生命周期**

```java
//将所选相机和任意用例绑定到生命周期 将 Preview 连接到 PreviewView
var camera: Camera? = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, imageAnalyzer)
```

**2.6 将 Preview 连接到 PreviewView**

```java
//将取景器的视图与预览用例进行绑定
preview?.setSurfaceProvider(viewFinder.surfaceProvider)
```


现在，您已完成实现相机预览的操作。请构建您的应用，然后确认预览是否出现在您的应用中并能按预期工作。

**下面贴一下实现预览的完整代码**


```java
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
				//Log.d("分析图片用例返回的结果：", "$luma")
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
```

.

.

### ImageAnalyzer(分析图片用例)的使用介绍

**介绍**

>图片分析用例为您的应用提供可供 `CPU` 访问的图片来执行图片处理、计算机视觉或机器学习推断。应用会实现对每个帧运行的 `analyze()` 方法。

**使用**

**1. 创建图片分析用例对象**

> **图片分析可以分为两种模式：**
>-  **阻塞模式：** `ImageAnalysis.STRATEGY_BLOCK_PRODUCER `
>> **阻塞模式的说明：** 在此模式下，执行器会依序从相应相机接收帧；这意味着，如果 analyze() 方法所用的时间超过单帧在当前帧速率下的延迟时间，所接收的帧便可能不再是最新的帧，因为在该方法返回之前，新帧会被阻止进入流水线
>
>- **非阻塞模式：** `ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST`
>> **非阻塞模式的说明：** 在此模式下，执行程序在调用 `analyze()` 方法时会从相机接收最新的可用帧。如果此方法所用的时间超过单帧在当前帧速率下的延迟时间，它可能会跳过某些帧，以便 `analyze()` 在下一次接收数据时获取相机流水线中的最新可用帧

**使用代码如下：**

```java
/*
* 设置分析图片用例
*
* 作用：提供可供 CPU 访问的图片来执行图片处理、计算机视觉或机器学习推断
* */
imageAnalyzer = ImageAnalysis.Builder()
    .setTargetAspectRatio(screenAspectRatio)//指定分辨率
    .setTargetRotation(rotation)
    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)//图片分析模式
    .build()
    .also {
        it.setAnalyzer(cameraExecutor, LuminosityAnalyzer { luma ->

            //分析图片用例返回的结果
			//Log.d("分析图片用例返回的结果：", "$luma")
        })
    }
```

**2. 创建图像分析类**

通过创建自定义图像分析类 `LuminosityAnalyzer`来进行图像的处理，并把处理的结果返回给分析图片用例，这里主要是计算出图像的亮度的处理代码如下：

```java
/** 用于分析用例回调的助手类型别名 */
typealias LumaListener = (luma: Double) -> Unit

/**
 * 自定义图像分析类.
 *
 * 作用：提供可供 CPU 访问的图片来执行图片处理、计算机视觉或机器学习推断
 *
 * 通过观察YUV格式的数据里的Y平面来计算图像的平均光度
 */
class LuminosityAnalyzer(listener: LumaListener? = null) : ImageAnalysis.Analyzer {

    private val frameRateWindow = 8
    private val frameTimestamps = ArrayDeque<Long>(5)//存放时间戳数组
    private val listeners = ArrayList<LumaListener>().apply { listener?.let { add(it) } }//存放监听回调的数组
    private var lastAnalyzedTimestamp = 0L//最后分析的时间戳
    var framesPerSecond: Double = -1.0//帧频
    private set

    /**
     * 用于添加将在计算每个luma时调用的侦听器
     */
    fun onFrameAnalyzed(listener: LumaListener) = listeners.add(listener)

    //用于从图像平面缓冲区提取字节数组
    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()    //将缓冲区归零，刷新缓冲区
        val data = ByteArray(remaining())
        get(data)   //将缓冲区复制到字节数组中
        return data //返回字节数组
    }

    /**
    * analyze(image: ImageProxy)方法：https://developer.android.google.cn/reference/androidx/camera/core/ImageAnalysis.Analyzer#analyze(androidx.camera.core.ImageProxy)
    *
    * image参数：格式ImageFormat.YUV_420_888
    *
    * */
    override fun analyze(image: ImageProxy) {

        //如果没有附加侦听器，我们就不需要执行分析
        if (listeners.isEmpty()) {
            image.close()
            return
        }

        //跟踪分析的帧
        val currentTime = System.currentTimeMillis()//获取系统的时间戳
        frameTimestamps.push(currentTime)

        //使用移动平均线计算FPS
        while (frameTimestamps.size >= frameRateWindow) frameTimestamps.removeLast()

        val timestampFirst = frameTimestamps.peekFirst() ?: currentTime

        val timestampLast = frameTimestamps.peekLast() ?: currentTime
        framesPerSecond = 1.0 / ((timestampFirst - timestampLast) / frameTimestamps.size.coerceAtLeast(1).toDouble()) * 1000.0

        //分析可能会花费任意长的时间，因为我们在不同的线程中运行，它不会让其他用例停顿
        lastAnalyzedTimestamp = frameTimestamps.first

        //由于图像分析的格式是YUV，图像[0]包含亮度平面
        val buffer = image.planes[0].buffer

        //从回调对象中提取图像数据
        val data = buffer.toByteArray()

        //将数据转换为0-255像素值的数组
        val pixels = data.map { it.toInt() and 0xFF }

        // 计算图像的平均亮度
        val luma = pixels.average()

        //使用新值调用所有侦听器
        listeners.forEach { it(luma) }

        //处理完一定要关闭（否则可能会阻止生成其他图像（导致预览停滞）或丢弃图像）
        image.close()
    }
}
```

.

.
### ImageCapture(图片拍摄用例)的使用介绍

**介绍**

> 图片拍摄用例旨在拍摄高分辨率的优质照片，不仅提供简单的相机手动控制功能，还提供自动白平衡、自动曝光和自动对焦 (3A) 功能。

**使用**

**1. 配置应用以拍摄照片的属性**

提供了拍照所需的基本控制功能。照片是使用闪光灯选项和连续自动对焦拍摄的。

```java
/*
* 设置图片拍摄用例
* ImageCapture的介绍：https://developer.android.google.cn/reference/androidx/camera/core/ImageCapture?hl=zh_cn#CAPTURE_MODE_MINIMIZE_LATENCY
* */
imageCapture = ImageCapture.Builder()
    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)//设置拍照模式
    .setTargetAspectRatio(screenAspectRatio)
    .setTargetRotation(rotation)//设置旋转
    .build()
```

**2. 配置好相机后，以下代码会根据用户操作拍照：**

```java
//拍照的方法
private fun TakeAPicture() {

    // 获取可修改图像捕获用例的稳定参考
    imageCapture?.let { imageCapture ->

        // 创建文件对象来保存图像
        val photoFile = createFile(outputDirectory, FILENAME, PHOTO_EXTENSION)

        // 设置图像捕获元数据
        val metadata = ImageCapture.Metadata().apply {

            //使用前置摄像头时的镜像
            isReversedHorizontal = lensFacing == CameraSelector.LENS_FACING_FRONT
        }

        // 创建输出选项对象（file：输出的文件；metadata：照片的元数据）
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile)
            .setMetadata(metadata)
            .build()

        // 设置图像捕捉监听器，在照片拍摄完成后触发
        imageCapture.takePicture(outputOptions, cameraExecutor, object : ImageCapture.OnImageSavedCallback {


            //拍照成功的回调（保存照片）
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {

                //照片的存储路径
                val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
                val savedUrl = savedUri.toString()

                //使用Handler发送消息
                val msg: Message = Message.obtain()
                msg.what = 1
                msg.obj = savedUrl
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
```

这样用户就可以实现拍摄的操作，这里将拍摄的照片存储到本地，并存储为`.jpg`格式，有可以在 `onImageSaved()` 方法中对输出的文件进行处理以满足你的需求。

.

.

### 供应商扩展的介绍

`CameraX` 会提供一个 `API`，用于访问手机制造商已为特定手机实现的效果（`焦外成像`、`HDR` 及其他功能）。

> **补充：**
> 
> 为了支持供应商扩展，设备必须满足以下所有条件：
>- 相应效果拥有来自设备 `OEM` 的库支持 
>- 当前设备上已安装了 `OEM` 库
>- `OEM` 库报告设备支持扩展
>- 设备搭载了库所要求的操作系统版本
>
>注意：设备可能有供应商支持，但未从 `OTA` 收到库。或者，设备可能同时有供应商支持和库，但尚未升级到受支持的最低版本。在这两种情况下，设备都有可能不支持供应商扩展。

由于这些功能的实现需要 `OEM` 厂商提供的库才能实现，而每一个 `OEM` 厂商提供的库都不同，使用这里无法展示具体的效果，只是介绍有这个扩展功能，如果真的要使用，请阅读 [**Google的使用介绍**](https://developer.android.google.cn/training/camerax/vendor-extensions)。


.

.

### 项目地址

- [**码云地址**](https://gitee.com/qu-wenbin/camera-x-demo)

- [**Github地址**](https://github.com/Ou520/CameraX-Demo)
