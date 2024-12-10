package com.example.veggievisionapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.ListView
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.veggievisionapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var searchView: SearchView
    private lateinit var listView: ListView
    private lateinit var captureButton: Button
    private lateinit var uploadButton: Button
    private lateinit var buttonPredict: Button
    private lateinit var predictions: TextView
    private var REQUEST_CAMERA_PERMISSION = 1
    private var REQUEST_IMAGE_CAPTURE = 2
    private var REQUEST_UPLOAD_PERMISSION = 3
    private lateinit var imageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize views
        searchView = binding.svVegNames
        listView = binding.lvVegNames
        buttonPredict = binding.btnPredict
        captureButton = binding.btnCaptureImage
        uploadButton = binding.btnUploadImage
        imageView = binding.imageView  // Initialize imageView here
        predictions = binding.tvPredictions

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        captureButton.setOnClickListener { captureImage(it) }
        uploadButton.setOnClickListener { uploadImage(it) }
        buttonPredict.setOnClickListener { predictModel(it) }

        val vegList: ArrayList<String> = ArrayList()
        vegList.addAll(listOf("Avocado", "Beans", "Beet", "Bell Pepper", "Broccoli", "Brus Capusta", "Cabbage", "Carrot", "Cauliflower", "Celery", "Corn", "Cucumber", "Eggplant", "Fasol", "Garlic", "Hot Pepper", "Onion", "Peas", "Potato", "Pumpkin", "Rediska", "Redka", "Salad", "Squash-Patisson", "Tomato", "Vegetable Marrow"))

        val adapter = ArrayAdapter(this, androidx.appcompat.R.layout.support_simple_spinner_dropdown_item, vegList)
        listView.adapter = adapter

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(p0: String?): Boolean {
                if (vegList.contains(p0)) {
                    adapter.filter.filter(p0)
                } else {
                    Toast.makeText(this@MainActivity, "Doesn't exist", Toast.LENGTH_SHORT).show()
                }
                return true
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                adapter.filter.filter(p0)
                binding.lvVegNames.visibility = View.VISIBLE
                return true
            }
        })

        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedItem = adapter.getItem(position)
            searchView.setQuery(selectedItem, false)
        }
    }

    private fun captureImage(view: View) {
        // Check for permission at runtime
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
            return
        }
        // Open the camera app
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
    }

    private fun uploadImage(view: View) {
        val intent = Intent()
        intent.action = Intent.ACTION_GET_CONTENT
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_UPLOAD_PERMISSION)
    }

    private fun predictModel(view: View){
        val vegName = searchView.query
        binding.tvPredictions.text="$vegName is detected in the image \nThere are 2 $vegName(s) in the given image. \nEstimated Weight: 500g"

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                captureImage(View(this))  // Pass a dummy view, because the actual view is not needed here
            } else {
                // Permission denied
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            val extras = data?.extras
            val imageBitmap = extras?.get("data") as Bitmap
            if (::imageView.isInitialized) {
                imageView.setImageBitmap(imageBitmap)
            } else {
                Log.e("MainActivity", "imageView is not initialized")
            }
        } else if (requestCode == REQUEST_UPLOAD_PERMISSION && resultCode == RESULT_OK) {
            val imageUri = data?.data
            if (imageUri != null && ::imageView.isInitialized) {
                imageView.setImageURI(imageUri)
            } else {
                Log.e("MainActivity", "imageView is not initialized or imageUri is null")
            }
        }
    }
}
