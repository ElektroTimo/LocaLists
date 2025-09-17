package com.example.localists.lists

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.localists.TaskItemClickListener
import com.example.localists.TaskItem
import com.example.localists.NewTaskSheet
import com.example.localists.TaskItemAdapter
import com.example.localists.TaskViewModel
import com.example.localists.databinding.FragmentTaskListBinding

class ListFragment : Fragment(), TaskItemClickListener {

    private var _binding: FragmentTaskListBinding? = null
    private val binding get() = _binding!!
    private lateinit var taskViewModel: TaskViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTaskListBinding.inflate(inflater, container, false)
        Log.d("ListFragment", "onCreateView: Binding inflated")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        taskViewModel = ViewModelProvider(requireActivity()).get(TaskViewModel::class.java)
        Log.d("ListFragment", "onViewCreated: ViewModel initialized, childFragmentManager: ${childFragmentManager}")

        if (binding.newTaskButton == null) {
            Log.e("ListFragment", "newTaskButton is null, check fragment_task_list.xml")
        } else {
            binding.newTaskButton.setOnClickListener {
                Log.d("ListFragment", "newTaskButton clicked, button enabled: ${it.isEnabled}")
                try {
                    Log.d("ListFragment", "Attempting to show NewTaskSheet with childFragmentManager: $childFragmentManager")
                    NewTaskSheet(null).show(childFragmentManager, "newTaskTag")
                    Log.d("ListFragment", "NewTaskSheet shown successfully")
                } catch (e: Exception) {
                    Log.e("ListFragment", "Error showing NewTaskSheet: ${e.message}, StackTrace: ${e.stackTraceToString()}")
                }
            }
        }

        setRecyclerView()
    }

    private fun setRecyclerView() {
        val fragment = this
        taskViewModel.taskItems.observe(viewLifecycleOwner) {
            if (binding.todoListRecyclerView != null) {
                binding.todoListRecyclerView.apply {
                    layoutManager = LinearLayoutManager(requireContext())
                    adapter = TaskItemAdapter(it, fragment)
                }
                Log.d("ListFragment", "RecyclerView updated with ${it.size} items")
            } else {
                Log.e("ListFragment", "todoListRecyclerView is null, check fragment_task_list.xml")
            }
        }
    }

    override fun editTaskItem(taskItem: TaskItem) {
        try {
            Log.d("ListFragment", "Editing task: ${taskItem.name}")
            NewTaskSheet(taskItem).show(childFragmentManager, "newTaskTag")
        } catch (e: Exception) {
            Log.e("ListFragment", "Error showing NewTaskSheet for edit: ${e.message}")
        }
    }

    override fun completeTaskItem(taskItem: TaskItem) {
        taskViewModel.setCompleted(taskItem)
        Log.d("ListFragment", "Task completed: ${taskItem.name}")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}