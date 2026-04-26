package com.iie.vaultquest.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.iie.vaultquest.data.AppDatabase
import com.iie.vaultquest.data.Category
import com.iie.vaultquest.databinding.ActivityCategoryBinding
import kotlinx.coroutines.launch

class CategoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCategoryBinding
    private val db by lazy { AppDatabase.getDatabase(this) }
    private var userId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userId = intent.getLongExtra("USER_ID", -1)
        if (userId == -1L) finish()

        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }

        binding.categoryList.layoutManager = LinearLayoutManager(this)
        
        observeCategories()

        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }

        binding.btnAdd.setOnClickListener {
            val name = binding.categoryName.text.toString()
            if (name.isNotEmpty()) {
                lifecycleScope.launch {
                    db.appDao().insertCategory(Category(userId = userId, name = name))
                    binding.categoryName.text?.clear()
                    Toast.makeText(this@CategoryActivity, "Category added", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Enter a category name", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeCategories() {
        lifecycleScope.launch {
            db.appDao().getCategoriesForUser(userId).collect { categories ->
                binding.categoryList.adapter = CategoryAdapter(categories)
            }
        }
    }
}
