package com.example.localists

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.example.localists.databinding.FragmentNewTaskSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.io.File
import java.io.FileOutputStream
import java.time.LocalTime
import androidx.core.graphics.drawable.toBitmap

class NewTaskSheet(var taskItem: TaskItem?) : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentNewTaskSheetBinding
    private lateinit var taskViewModel: TaskViewModel
    private var dueTime: LocalTime? = null

    private val REQUEST_IMAGE_CAPTURE = 1


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity()

        if (taskItem != null) {
            binding.taskTitle.text = "Edit Task"
            val editable = Editable.Factory.getInstance()
            binding.name.text = editable.newEditable(taskItem!!.name)
            binding.desc.text = editable.newEditable(taskItem!!.desc)
            if (taskItem!!.dueTime != null) {
                dueTime = taskItem!!.dueTime!!
                updateTimeButtonText()
            }
        } else {
            binding.taskTitle.text = "New Task"
        }

        taskViewModel = ViewModelProvider(activity).get(TaskViewModel::class.java)
        binding.saveButton.setOnClickListener {
            saveAction()
        }
        binding.timePickerButton.setOnClickListener {
            openTimePicker()
        }

        // Set up the onClickListener for the Capture Photo button
        binding.capturePhotoButton.setOnClickListener {
            dispatchTakePictureIntent()
        }
    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(requireActivity().packageManager)?.also {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            // Handle the captured image as needed, e.g., display it in an ImageView
            binding.taskImage.setImageBitmap(imageBitmap)
            Log.d("NewTaskSheet", "Image captured")
        }
    }

    private fun openTimePicker() {
        if (dueTime == null)
            dueTime = LocalTime.now()
        val listener = TimePickerDialog.OnTimeSetListener { _, selectedHour, selectedMinute ->
            dueTime = LocalTime.of(selectedHour, selectedMinute)
            updateTimeButtonText()
        }
        val dialog = TimePickerDialog(activity, listener, dueTime!!.hour, dueTime!!.minute, true)
        dialog.setTitle("Reminder for when?")
        dialog.show()
    }

    private fun updateTimeButtonText() {
        binding.timePickerButton.text = String.format("%02d:%02d", dueTime!!.hour, dueTime!!.minute)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentNewTaskSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun saveAction() {
        val name = binding.name.text.toString()
        val desc = binding.desc.text.toString()

        if (!(taskItem == null)) {
            val newTask = TaskItem(name, desc, dueTime, null)
            taskViewModel.addTaskItem(newTask)
            Log.d("NewTaskSheet", "New task added: $name")
        } else {
            taskViewModel.updateTaskItem(taskItem!!.id, name, desc, dueTime)
            Log.d("NewTaskSheet", "Task updated: $name")
        }

        // Save task items after adding or updating
        taskViewModel.saveTaskItems(requireContext())

        // Set image path for the task item if an image was taken
        val imageBitmap = binding.taskImage.drawable.toBitmap()
        if (imageBitmap != null) {
            val imageFilePath = saveImageToFile(imageBitmap)
            Log.d("SaveAction", "Image path: $imageFilePath") // Add this line for logging
            taskViewModel.setImagePath(taskItem!!.id, imageFilePath!!)
        }

        // Clear other fields and dismiss
        binding.name.setText("")
        binding.desc.setText("")
        binding.taskImage.setImageDrawable(null) // Clear the image view
        dismiss()
    }

    private fun saveImageToFile(bitmap: Bitmap): String {
        // Implement the logic to save the bitmap to a file
        // Return the file path
        // Example:
        val file = File(requireContext().cacheDir, "task_image.jpg")
        FileOutputStream(file).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        }
        return file.absolutePath
    }
}
