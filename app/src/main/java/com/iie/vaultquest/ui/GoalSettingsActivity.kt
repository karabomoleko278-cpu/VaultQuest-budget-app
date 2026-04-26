package com.iie.vaultquest.ui

import android.os.Bundle
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.iie.vaultquest.data.AppDatabase
import com.iie.vaultquest.data.Goal
import com.iie.vaultquest.databinding.ActivityGoalSettingsBinding
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class GoalSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGoalSettingsBinding
    private val db by lazy { AppDatabase.getDatabase(this) }
    private var userId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGoalSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userId = intent.getLongExtra("USER_ID", -1)
        if (userId == -1L) finish()

        setupSeekBars()
        loadGoals()

        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }

        binding.btnSaveGoals.setOnClickListener {
            val min = binding.seekBarMin.progress.toDouble()
            val max = binding.seekBarMax.progress.toDouble()

            if (min >= max) {
                Toast.makeText(this, "Max goal must be greater than min goal", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                db.appDao().setGoals(Goal(userId = userId, minGoal = min, maxGoal = max))
                Toast.makeText(this@GoalSettingsActivity, "Goals updated", Toast.LENGTH_SHORT).show()
                onBackPressedDispatcher.onBackPressed()
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
            }
        }
    }

    private fun setupSeekBars() {
        binding.seekBarMin.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) binding.minGoalValue.setText(progress.toString())
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.seekBarMax.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) binding.maxGoalValue.setText(progress.toString())
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Bidirectional sync: EditText updates SeekBar
        binding.minGoalValue.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val progress = binding.minGoalValue.text.toString().toIntOrNull() ?: 0
                binding.seekBarMin.progress = progress
            }
        }
        binding.maxGoalValue.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val progress = binding.maxGoalValue.text.toString().toIntOrNull() ?: 0
                binding.seekBarMax.progress = progress
            }
        }
    }

    private fun loadGoals() {
        lifecycleScope.launch {
            db.appDao().getGoalsForUser(userId).collect { goalsList ->
                val goal = goalsList.firstOrNull()
                if (goal != null) {
                    binding.seekBarMin.progress = goal.minGoal.toInt()
                    binding.seekBarMax.progress = goal.maxGoal.toInt()
                    binding.minGoalValue.setText(goal.minGoal.toInt().toString())
                    binding.maxGoalValue.setText(goal.maxGoal.toInt().toString())
                }
            }
        }
    }
}
