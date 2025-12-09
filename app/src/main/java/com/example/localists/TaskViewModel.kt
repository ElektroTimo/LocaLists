package com.example.localists

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.util.*
import android.util.Log
import org.json.JSONObject
import android.content.Context

class TaskViewModel : ViewModel() {
    var taskItems = MutableLiveData<MutableList<TaskItem>>().apply {
        value = mutableListOf()
    }
        private set

    fun saveTaskItems(context: Context) {
        val sharedPreferences = context.getSharedPreferences("TaskItemsPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Convert each TaskItem to JSON and store in a list
        val taskItemsJsonList = taskItems.value?.map { taskItem ->
            JSONObject().apply {
                put("name", taskItem.name)
                put("desc", taskItem.desc)
                put("dueTime", taskItem.dueTime?.toString()) // Convert LocalTime to string
                put("completedDate", taskItem.completedDate?.toString()) // Convert LocalDate to string
                put("id", taskItem.id.toString())
                put("imagePath", taskItem.imagePath) // Add imagePath to JSON
            }.toString()
        }

        // Store the list in SharedPreferences
        editor.putStringSet("taskItems", taskItemsJsonList?.toSet())
        editor.apply()
        Log.d("TaskViewModel", "Task items saved.")
    }

    fun loadTaskItems(context: Context) {
        val sharedPreferences = context.getSharedPreferences("TaskItemsPrefs", Context.MODE_PRIVATE)
        val taskItemsJsonList = sharedPreferences.getStringSet("taskItems", null)

        // Convert each JSON string back to TaskItem
        val loadedTaskItems = taskItemsJsonList?.map { taskItemJson ->
            val jsonObject = JSONObject(taskItemJson)
            TaskItem(
                name = jsonObject.getString("name"),
                desc = jsonObject.getString("desc"),
                dueTime = jsonObject.optString("dueTime")?.takeIf { it.isNotEmpty() }?.let { LocalTime.parse(it) },
                completedDate = jsonObject.optString("completedDate")?.takeIf { it.isNotEmpty() }?.let { LocalDate.parse(it) },
                id = UUID.fromString(jsonObject.getString("id")),
                imagePath = jsonObject.optString("imagePath")?.takeIf { it.isNotEmpty() } // Load imagePath
            )
        }

        taskItems.postValue(loadedTaskItems?.toMutableList() ?: mutableListOf())
        Log.d("TaskViewModel", "Task items loaded.")
    }

    fun addTaskItem(newTask: TaskItem) {
        val list = taskItems.value
        list?.add(newTask)
        taskItems.postValue(list)
        Log.d("TaskViewModel", "Task added. New size: ${list?.size}")
    }

    fun updateTaskItem(id: UUID, name: String?, desc: String?, dueTime: LocalTime?) {
        val list = taskItems.value
        val task = list?.find { it.id == id }
        if (task != null) {
            task.name = name ?: task.name
            task.desc = desc ?: task.desc
            task.dueTime = dueTime ?: task.dueTime
            taskItems.postValue(list)
        }
    }

    fun setCompleted(taskItem: TaskItem) {
        val list = taskItems.value
        val task = list?.find { it.id == taskItem.id }
        if (task != null && task.completedDate == null) {
            task.completedDate = LocalDate.now()
            taskItems.postValue(list)
        }
    }

    fun setImagePath(taskItemId: UUID, imagePath: String) {
        // Find the task with the given ID and update its image path
        taskItems.value?.let { list ->
            list.find { it.id == taskItemId }?.let { task ->
                task.imagePath = imagePath
                taskItems.postValue(list)
            }
        }
    }

    fun getImagePath(taskItemId: UUID): String? {
        // Find the task with the given ID and return its image path
        return taskItems.value?.find { it.id == taskItemId }?.imagePath
    }
}