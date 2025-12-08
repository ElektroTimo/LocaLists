// File: app/src/main/java/com/example/localists/TaskViewModel.kt
package com.example.localists

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

class TaskViewModel : ViewModel() {

    var taskItems = MutableLiveData<MutableList<TaskItem>>().apply {
        value = mutableListOf()
    }

    fun saveTaskItems(context: Context) {
        val sharedPreferences = context.getSharedPreferences("TaskItemsPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        val taskItemsJsonList = taskItems.value?.map { taskItem ->
            JSONObject().apply {
                put("name", taskItem.name)
                put("desc", taskItem.desc)
                put("dueTime", taskItem.dueTime?.toString())
                put("completedDate", taskItem.completedDate?.toString())
                put("id", taskItem.id.toString())
                put("imagePath", taskItem.imagePath ?: "")
                put("latitude", taskItem.latitude ?: JSONObject.NULL)
                put("longitude", taskItem.longitude ?: JSONObject.NULL)
                put("radiusMeters", taskItem.radiusMeters ?: JSONObject.NULL)
            }.toString()
        }

        editor.putStringSet("taskItems", taskItemsJsonList?.toSet())
        editor.apply()
        Log.d("TaskViewModel", "Task items saved.")
    }

    fun loadTaskItems(context: Context) {
        val sharedPreferences = context.getSharedPreferences("TaskItemsPrefs", Context.MODE_PRIVATE)
        val taskItemsJsonList = sharedPreferences.getStringSet("taskItems", null)

        val loadedTaskItems = taskItemsJsonList?.map { jsonString ->
            val json = JSONObject(jsonString)
            TaskItem(
                name = json.getString("name"),
                desc = json.getString("desc"),
                dueTime = json.optString("dueTime").takeIf { it.isNotEmpty() }?.let { LocalTime.parse(it) },
                completedDate = json.optString("completedDate").takeIf { it.isNotEmpty() }?.let { LocalDate.parse(it) },
                id = UUID.fromString(json.getString("id")),
                imagePath = json.optString("imagePath").takeIf { it.isNotEmpty() },
                latitude = if (json.isNull("latitude")) null else json.getDouble("latitude"),
                longitude = if (json.isNull("longitude")) null else json.getDouble("longitude"),
                radiusMeters = if (json.isNull("radiusMeters")) null else json.getDouble("radiusMeters").toFloat()
            )
        }?.toMutableList() ?: mutableListOf()

        taskItems.postValue(loadedTaskItems)
        Log.d("TaskViewModel", "Task items loaded.")
    }

    fun addTaskItem(newTask: TaskItem) {
        val list = taskItems.value ?: mutableListOf()
        list.add(newTask)
        taskItems.postValue(list)
    }

    fun updateTaskItem(id: UUID, name: String?, desc: String?, dueTime: LocalTime?) {
        val list = taskItems.value ?: return
        list.find { it.id == id }?.let { task ->
            name?.let { task.name = it }
            desc?.let { task.desc = it }
            dueTime?.let { task.dueTime = it }
            taskItems.postValue(list)
        }
    }

    fun setCompleted(taskItem: TaskItem) {
        val list = taskItems.value ?: return
        list.find { it.id == taskItem.id }?.let { task ->
            if (task.completedDate == null) {
                task.completedDate = LocalDate.now()
                taskItems.postValue(list)
            }
        }
    }

    fun setImagePath(taskItemId: UUID, imagePath: String) {
        taskItems.value?.find { it.id == taskItemId }?.imagePath = imagePath
        taskItems.postValue(taskItems.value)
    }

    // NEW: Time-based alarm scheduling
    fun scheduleTimeAlarm(context: Context, taskItem: TaskItem) {
        if (taskItem.dueTime == null) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("taskName", taskItem.name)
            putExtra("taskDesc", taskItem.desc.ifEmpty { "Reminder time!" })
            putExtra("taskId", taskItem.id.toString())
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskItem.id.toString().hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val trigger = LocalDateTime.now()
            .withHour(taskItem.dueTime.hour)
            .withMinute(taskItem.dueTime.minute)
            .withSecond(0)
            .withNano(0)

        val triggerMillis = if (trigger.isBefore(LocalDateTime.now())) {
            trigger.plusDays(1)
        } else {
            trigger
        }.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        Log.d("TaskViewModel", "Scheduled alarm for '${taskItem.name}' at $trigger")
    }

    fun cancelTimeAlarm(context: Context, taskItem: TaskItem) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskItem.id.toString().hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.d("TaskViewModel", "Cancelled alarm for '${taskItem.name}'")
    }
}