package com.example.localists

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View // NEW: For visibility changes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider // NEW: For TaskViewModel
import com.example.localists.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var taskViewModel: TaskViewModel // NEW: Activity-scoped ViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // NEW: Hide toolbar to remove top bar (no action bar needed)
        binding.toolbar.visibility = View.GONE

        // Comment out drawer and nav setup to disable hamburger menu
        // val drawerLayout = binding.drawerLayout
        // val navController = findNavController(R.id.nav_host_fragment_content_main)
        // appBarConfiguration = AppBarConfiguration(
        //     setOf(R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow),
        //     drawerLayout
        // )
        // setupActionBarWithNavController(navController, appBarConfiguration)
        // binding.navView.setupWithNavController(navController)
        //
        // val toggle = ActionBarDrawerToggle(
        //     this, drawerLayout, binding.toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        // )
        // drawerLayout.addDrawerListener(toggle)
        // toggle.syncState()

        createNotificationChannel()
        requestPermissions()

        // NEW: Init ViewModel and load tasks (for list in fragment)
        taskViewModel = ViewModelProvider(this).get(TaskViewModel::class.java)
        taskViewModel.loadTaskItems(this)

        // Test notification on app launch (comment out after testing)
        sendTaskNotification("Test Task", "This is a test task", "test123")
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            else -> super.onOptionsItemSelected(item)
        }
    }

    // override fun onSupportNavigateUp(): Boolean {
    //     val navController = findNavController(R.id.nav_host_fragment_content_main)
    //     return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    // }

    private fun requestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.POST_NOTIFICATIONS
        )
        val toRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (toRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, toRequest, 100)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            permissions.forEachIndexed { index, permission ->
                if (grantResults[index] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("MainActivity", "Permission granted: $permission")
                } else {
                    Log.d("MainActivity", "Permission denied: $permission")
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Localists Channel"
            val descriptionText = "Channel for Localists notifications"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("LOCALISTS_CHANNEL_ID", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendTaskNotification(taskName: String, taskDesc: String, taskId: String) {
        val intent = Intent(this, TaskNotificationReceiver::class.java).apply {
            putExtra("taskName", taskName)
            putExtra("taskDesc", taskDesc)
            putExtra("taskId", taskId)
        }
        sendBroadcast(intent)
        Log.d("MainActivity", "Broadcast sent for task: $taskName")
    }
}