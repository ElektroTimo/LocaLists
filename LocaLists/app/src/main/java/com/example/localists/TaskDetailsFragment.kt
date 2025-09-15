package com.example.localists

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.localists.databinding.FragmentTaskDetailsBinding
import java.util.UUID // Import UUID here
import android.net.Uri


class TaskDetailsFragment : Fragment() {

    private lateinit var binding: FragmentTaskDetailsBinding
    private lateinit var taskViewModel: TaskViewModel
    private lateinit var taskId: UUID // You need to get the task ID from arguments or other sources

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentTaskDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        taskViewModel = ViewModelProvider(requireActivity()).get(TaskViewModel::class.java)

        // Get the task ID from arguments or other sources
        taskId = arguments?.getSerializable("taskId") as UUID
            ?: throw IllegalArgumentException("Task ID must be provided")

        // Observe the task details from the ViewModel
        taskViewModel.taskItems.observe(viewLifecycleOwner, { taskItems ->
            // Find the task with the specified ID
            val taskItem = taskItems.find { it.id == taskId }

            // Update the UI with task details
            updateUI(taskItem)
        })
    }

    // Inside the updateUI() function in TaskDetailsFragment
    private fun updateUI(taskItem: TaskItem?) {
        // Update the UI with task details
        binding.detailTitle.text = taskItem?.name
        binding.detailDesc.text = "Description: ${taskItem?.desc}"

        // Set image using setImageURI if imagePath is not null
        taskItem?.imagePath?.let { imagePath ->
            binding.detailImage.setImageURI(Uri.parse(imagePath))
        }

        // Show or hide due time based on its presence
        if (taskItem?.dueTime != null) {
            binding.detailDueTime.text = "Due Time: ${taskItem.dueTime}"
            binding.detailDueTime.visibility = View.VISIBLE
        } else {
            binding.detailDueTime.visibility = View.GONE
        }
    }

}
