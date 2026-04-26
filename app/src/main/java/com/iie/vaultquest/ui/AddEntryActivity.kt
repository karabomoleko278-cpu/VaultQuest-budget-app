package com.iie.vaultquest.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.iie.vaultquest.data.AppDatabase
import com.iie.vaultquest.data.Category
import com.iie.vaultquest.data.Entry
import com.iie.vaultquest.databinding.ActivityAddEntryBinding
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "AddEntryActivity"

class AddEntryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEntryBinding
    private val db by lazy { AppDatabase.getDatabase(this) }
    private var userId: Long = -1
    private var categories = listOf<Category>()
    
    private var selectedDate = Calendar.getInstance()
    private var startTime = ""
    private var endTime = ""
    private var photoPath: String? = null

    private val takePhotoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            binding.photoPreview.visibility = View.VISIBLE
            binding.photoPreview.setImageURI(Uri.fromFile(File(photoPath!!)))
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            dispatchTakePictureIntent()
        } else {
            Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userId = intent.getLongExtra("USER_ID", -1)
        if (userId == -1L) finish()

        setupPickers()
        loadCategories()

        binding.btnBack.setOnClickListener { 
            onBackPressedDispatcher.onBackPressed()
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }
        binding.btnSave.setOnClickListener { saveEntry() }
    }

    private fun setupPickers() {
        binding.btnPickDate.setOnClickListener {
            DatePickerDialog(this, { _, y, m, d ->
                selectedDate.set(y, m, d)
                updateSummary()
            }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH)).show()
        }

        binding.btnPickStartTime.setOnClickListener {
            TimePickerDialog(this, { _, h, min ->
                startTime = String.format("%02d:%02d", h, min)
                updateSummary()
            }, 12, 0, true).show()
        }

        binding.btnPickEndTime.setOnClickListener {
            TimePickerDialog(this, { _, h, min ->
                endTime = String.format("%02d:%02d", h, min)
                updateSummary()
            }, 13, 0, true).show()
        }
    }

    private fun updateSummary() {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        binding.dateTimeSummary.text = "Date: ${sdf.format(selectedDate.time)} | Start: $startTime | End: $endTime"
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            db.appDao().getCategoriesForUser(userId).collect { list ->
                if (list.isEmpty()) {
                    // Predefined categories for a better start
                    val defaults = listOf("Food", "Transport", "Rent", "Groceries", "Entertainment", "Savings", "Emergency")
                    defaults.forEach { 
                        db.appDao().insertCategory(Category(userId = userId, name = it))
                    }
                    return@collect // Re-trigger via collector
                }
                categories = list
                val adapter = ArrayAdapter(this@AddEntryActivity, android.R.layout.simple_spinner_item, list.map { it.name })
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.categorySpinner.adapter = adapter
            }
        }
    }

    private fun dispatchTakePictureIntent() {
        Log.d(TAG, "Dispatching camera intent")
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val file = File.createTempFile("IMG_${System.currentTimeMillis()}", ".jpg", storageDir)
        photoPath = file.absolutePath
        val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
        
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, uri)
        }
        takePhotoLauncher.launch(intent)
    }

    private fun saveEntry() {
        val amountStr = binding.amount.text.toString()
        val desc = binding.description.text.toString()
        val catIndex = binding.categorySpinner.selectedItemPosition

        Log.d(TAG, "Saving entry: $desc, Amount: $amountStr")

        if (amountStr.isEmpty() || desc.isEmpty() || catIndex == -1 || startTime.isEmpty() || endTime.isEmpty()) {
            Log.w(TAG, "Save failed: Validation error")
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val entry = Entry(
                    userId = userId,
                    categoryId = categories[catIndex].id,
                    date = selectedDate.timeInMillis,
                    startTime = startTime,
                    endTime = endTime,
                    description = desc,
                    amount = amountStr.toDouble(),
                    photoPath = photoPath
                )
                db.appDao().insertEntry(entry)
                Log.i(TAG, "Entry saved successfully to RoomDB")
                Toast.makeText(this@AddEntryActivity, "💰 Ka-ching! Entry saved!", Toast.LENGTH_LONG).show()
                finish()
            } catch (e: Exception) {
                Log.e(TAG, "Error saving entry: ${e.message}")
            }
        }
    }
}
