package com.example.sketchbook

import android.Manifest
import android.Manifest.permission.*
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.Image
import android.media.MediaScannerConnection

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.FrameLayout

import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher

import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception


class MainActivity : AppCompatActivity() {

    private val openGalleryLauncher : ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()){
            result->
            if(result.resultCode == RESULT_OK && result.data != null){
                val imageBg = findViewById<ImageView>(R.id.img_background)
                imageBg.setImageURI(result.data?.data)
            }
        }

    private val storageResultLauncher : ActivityResultLauncher<String> =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()){
                isGranted ->
                    if(isGranted){
                      //  Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()

                        val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        openGalleryLauncher.launch(pickIntent)
                    }else{
                        Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                    }
            }



    private var drawingView : SketchView ?= null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
         val brush = findViewById<ImageButton>(R.id.ib_brush)
        brush.setOnClickListener{
            showBrushSizeDialog()
        }

        val btnStoragePermission : ImageButton= findViewById(R.id.img_Button)

        btnStoragePermission.setOnClickListener{
            askPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val saveStoragePermission : ImageButton = findViewById(R.id.save_Button)

        saveStoragePermission.setOnClickListener{
            askPermission(WRITE_EXTERNAL_STORAGE)
        }

        val red = findViewById<ImageButton>(R.id.ib_red_dialog)
        val blue = findViewById<ImageButton>(R.id.ib_blue_dialog)
        val black = findViewById<ImageButton>(R.id.ib_black_dialog)
        val green = findViewById<ImageButton>(R.id.ib_green_dialog)
        val yellow = findViewById<ImageButton>(R.id.ib_yellow_dialog)
        val white = findViewById<ImageButton>(R.id.ib_white_dialog)

        red.setOnClickListener{
            drawingView?.setColor("Red")
        }
        blue.setOnClickListener{
            drawingView?.setColor("Blue")
        }
        black.setOnClickListener{
            drawingView?.setColor("Black")
        }
        green.setOnClickListener{
            drawingView?.setColor("Green")
        }
        yellow.setOnClickListener{
            drawingView?.setColor("#FFA500")
        }
        white.setOnClickListener {
            drawingView?.setColor("#FFFFFF")
        }

        val ib_save = findViewById<ImageButton>(R.id.save_Button)
        ib_save.setOnClickListener {
            if(isReadStorageAllowed()){
                lifecycleScope.launch(){
                    val fullDrawingView : FrameLayout = findViewById(R.id.frame_layout)
                    val bitmap = getBitmapFromView(fullDrawingView)
                    saveBitmapFile(bitmap)

                }
            }
        }


//        val colo = findViewById(R.id.ib_color)
//        color.setOnClickListener{
//            showColorSelectDialog()
//        }

        val back = findViewById<ImageButton>(R.id.back_Button)
        back.setOnClickListener{
            remove()
        }

        val redoBtn = findViewById<ImageButton>(R.id.redo)
        redoBtn.setOnClickListener {
            add()
        }


        drawingView = findViewById(R.id.sketchView)
        drawingView?.setBrushSize(10.toFloat())
        drawingView?.setColor("Black")
    }



    private fun showBrushSizeDialog(){

        val brushDialog = Dialog(this)

        brushDialog.setContentView(R.layout.brush_size_dialog)

        brushDialog.setTitle("Brush Size : ")
        val smallBtn = brushDialog.findViewById<ImageButton>(R.id.ib_brushSize_Small)
        val mediumBtn = brushDialog.findViewById<ImageButton>(R.id.ib_brushSize_Medium)
        val largeBtn = brushDialog.findViewById<ImageButton>(R.id.ib_brushSize_large)

        smallBtn.setOnClickListener(View.OnClickListener {
            drawingView?.setBrushSize(10.toFloat())
            brushDialog.dismiss()
        })

        mediumBtn.setOnClickListener(View.OnClickListener {
            drawingView?.setBrushSize(20.toFloat())
            brushDialog.dismiss()
        })
        largeBtn.setOnClickListener(View.OnClickListener {
            drawingView?.setBrushSize(30.toFloat())
            brushDialog.dismiss()
        })
        brushDialog.show()

    }

    private fun remove(){
        drawingView?.removePath()
    }
    private fun add(){
        drawingView?.redo()
    }

    private fun showRationaleDialog(title:String, message:String){
        val builder : AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("Dismiss"){
                dialog, _->
                    dialog.dismiss()
            }
        builder.show()
    }

    private fun askPermission(per : String){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            shouldShowRequestPermissionRationale(per)){
                if(per == READ_EXTERNAL_STORAGE){
                    showRationaleDialog("Permission required for accessing storage",
                        "You can not import image because storage access permission is denied")
                }
            else{
                showRationaleDialog("Permission reuired for accessing storage",
                "You can not save image because storage access permission is denied")
            }

        }
        else{
            storageResultLauncher.launch(per)
        }
    }

    private fun getBitmapFromView(view : View) : Bitmap {
        val bitmap = Bitmap.createBitmap(view.width,view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val bgDrawable = view.background
        if(bgDrawable != null){
            bgDrawable.draw(canvas)
        }
        else{
            canvas.drawColor(Color.WHITE)
        }

        view.draw(canvas)
        return bitmap
    }

    private suspend fun saveBitmapFile(myBitmap : Bitmap?):String{
        var result = ""
        withContext(Dispatchers.IO){
            if(myBitmap != null){
                try{
                    val bytes = ByteArrayOutputStream()
                    myBitmap.compress(Bitmap.CompressFormat.PNG,90,bytes)

                    val f = File(externalCacheDir?.absoluteFile.toString()
                    + File.separator + "SketchBook_" + System.currentTimeMillis() / 1000 + ".png")

                    val fo = FileOutputStream(f)
                    fo.write(bytes.toByteArray())
                    fo.close()

                    result = f.absolutePath

                    runOnUiThread {
                        if(result.isNotEmpty()){
                            Toast.makeText(this@MainActivity, "File Saved Successfully :$result", Toast.LENGTH_SHORT).show()
                            //shareImage(result)
                        }else{
                            Toast.makeText(this@MainActivity, "Something wnt wrong", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                catch(e:Exception){
                    result = ""
                    e.printStackTrace()
                }
            }
        }

        return result
    }

    private fun isReadStorageAllowed() :Boolean{
        val result = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)

        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun shareImage(result: String){
        MediaScannerConnection.scanFile(this, arrayOf(result),null){
            path, uri->
                val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM,uri)
            shareIntent.type = "image.png"
            startActivity(Intent.createChooser(shareIntent,"Share"))
        }
    }


    //    private fun showColorSelectDialog(){
//        val colorDialog = Dialog(this)
//        colorDialog.setContentView(R.layout.color_select_dialog)
//
//        colorDialog.setTitle("Select Color")
//        val red = colorDialog.findViewById<ImageButton>(R.id.ib_red_dialog)
//        val blue = colorDialog.findViewById<ImageButton>(R.id.ib_blue_dialog)
//        val green = colorDialog.findViewById<ImageButton>(R.id.ib_green_dialog)
//        val black = colorDialog.findViewById<ImageButton>(R.id.ib_black_dialog)
//
//        red.setOnClickListener(View.OnClickListener{
//            drawingView?.setColor("Red")
//            colorDialog.dismiss()
//        })
//
//        blue.setOnClickListener(View.OnClickListener{
//            drawingView?.setColor("Blue")
//            colorDialog.dismiss()
//        })
//
//        green.setOnClickListener(View.OnClickListener {
//            drawingView?.setColor("Green")
//            colorDialog.dismiss()
//        })
//
//        black.setOnClickListener(View.OnClickListener {
//            drawingView?.setColor("Black")
//            colorDialog.dismiss()
//        })
//
//        colorDialog.show()
//
//    }
}