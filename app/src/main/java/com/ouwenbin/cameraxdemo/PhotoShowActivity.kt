package com.ouwenbin.cameraxdemo



import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.PhotoView
import java.io.File


/** 照片的显示页面 */
class PhotoShowActivity : AppCompatActivity() {

    private lateinit var photoView: PhotoView
    private lateinit var ib_back:ImageButton
    private lateinit var delete_button:ImageButton
    var url:String? = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_show)

        initView()
        initData()
        initListener()

    }

    private fun initView() {
        photoView = findViewById(R.id.photo_view)
        ib_back = findViewById(R.id.ib_back)
        delete_button = findViewById(R.id.delete_button)

    }

    private fun initData() {

        //接收Intent传过来的网址
        val intent = intent
        url = intent.getStringExtra("urlKey") //图片地址
        Glide.with(this).load(url).into(photoView)

    }

    private fun initListener() {

        //返回按钮
        ib_back.setOnClickListener(View.OnClickListener {
            finish()
        })

        //删除按钮
        delete_button.setOnClickListener(View.OnClickListener {


            when {
                url !="" -> {

                    /*-----------删除图片-------------*/
                    val f = File(url)
                    if (f.exists()){

                        f.delete()//删除文件

                        //通知图库文件已经删除
                        Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).also { mediaScanIntent ->
                            mediaScanIntent.data = Uri.fromFile(f)
                            sendBroadcast(mediaScanIntent)
                        }
                    }

                    Toast.makeText(this,"照片删除成功!",Toast.LENGTH_SHORT).show()


                    //让intent携带数据跳转到ImgShow
                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra("shanchu","shanchu")
                    startActivity(intent)

                }
            }
        })

    }
}