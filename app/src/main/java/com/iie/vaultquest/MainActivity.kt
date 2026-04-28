package com.iie.vaultquest

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.iie.vaultquest.data.AppDatabase
import com.iie.vaultquest.databinding.ActivityMainBinding
import com.iie.vaultquest.ui.*
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val db by lazy { AppDatabase.getDatabase(this) }
    private var userId: Long = -1
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userId = intent.getLongExtra("USER_ID", -1)
        val username = intent.getStringExtra("USERNAME") ?: "User"

        if (userId == -1L) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding.welcomeText.text = "Welcome, $username"

        setupClickListeners()
        updateDashboard()
    }

    override fun onResume() {
        super.onResume()
        updateDashboard()
    }

    private fun setupClickListeners() {
        binding.btnLogout.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
        binding.btnAddEntry.setOnClickListener {
            startActivity(Intent(this, AddEntryActivity::class.java).putExtra("USER_ID", userId))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
        binding.btnViewEntries.setOnClickListener {
            startActivity(Intent(this, EntryListActivity::class.java).putExtra("USER_ID", userId))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
        binding.btnCategories.setOnClickListener {
            startActivity(Intent(this, CategoryActivity::class.java).putExtra("USER_ID", userId))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
        binding.btnGoals.setOnClickListener {
            startActivity(Intent(this, GoalSettingsActivity::class.java).putExtra("USER_ID", userId))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
        binding.btnReports.setOnClickListener {
            startActivity(Intent(this, ReportsActivity::class.java).putExtra("USER_ID", userId))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    private fun updateDashboard() {
        lifecycleScope.launch {
            combine(
                db.appDao().getEntriesForUser(userId),
                db.appDao().getGoalsForUser(userId)
            ) { entries, goalsList ->
            ).collect { (entries, goals) ->
                val totalSpent = entries.filter { isCurrentMonth(it.date) }.sumOf { it.amount }
                binding.totalSpent.text = currencyFormat.format(totalSpent)

                val totalBudget = goals.sumOf { it.amount }

                if (totalBudget > 0) {
                    val status = when {
                        totalSpent > totalBudget -> "📉 Over budget! Save more next month."
                        totalSpent < totalBudget * 0.5 -> "🏆 Savings Master: Under goal!"
                        else -> "⭐ On Track: Budgeting like a pro!"
                    }
                    binding.goalStatus.text = status
                    binding.goalStatus.setTextColor(if (totalSpent > totalBudget) getColor(R.color.error) else getColor(R.color.secondary))
                } else {
                    binding.goalStatus.text = "Goal: Not Set"
                }
            }
        }
    }

    private fun isCurrentMonth(date: Long): Boolean {
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)
        
        cal.timeInMillis = date
        return cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
    }
}
