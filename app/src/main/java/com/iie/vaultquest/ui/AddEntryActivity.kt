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

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri: Uri? = result.data?.data
            uri?.let {
                photoPath = it.toString()
                binding.photoPreview.visibility = View.VISIBLE
                binding.photoPreview.setImageURI(it)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            openGallery()
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userId = intent.getLongExtra("USER_ID", -1)
        if (userId == -1L) finish()

        if (savedInstanceState != null) {
            photoPath = savedInstanceState.getString("PHOTO_PATH")
            photoPath?.let { path ->
                binding.photoPreview.visibility = View.VISIBLE
                binding.photoPreview.setImageURI(Uri.parse(path))
            }
        }

        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }

        setupPickers()
        loadCategories()

        binding.btnTakePhoto.setOnClickListener {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
                    openGallery()
                } else {
                    requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                }
            } else {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    openGallery()
                } else {
                    requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
        }
        binding.btnSave.setOnClickListener { saveEntry() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("PHOTO_PATH", photoPath)
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
                endTime = startTime // Simplifying: just use one time
                updateSummary()
            }, 12, 0, true).show()
        }
    }

    private fun updateSummary() {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        binding.dateTimeSummary.text = "${sdf.format(selectedDate.time)} | $startTime"
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

    private fun openGallery() {
        Log.d(TAG, "Opening gallery")
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private fun saveEntry() {
        val amountStr = binding.amount.text.toString()
        val desc = binding.description.text.toString()
        val catIndex = binding.categorySpinner.selectedItemPosition

        Log.d(TAG, "Saving entry: $desc, Amount: $amountStr")

        if (amountStr.isEmpty() || desc.isEmpty() || catIndex == -1 || startTime.isEmpty()) {
            Log.w(TAG, "Save failed: Validation error")
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
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
