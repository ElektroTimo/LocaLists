package com.example.localists.lists

import android.os.Bundle
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

class FragmentTaskList : Fragment(), TaskItemClickListener {

    private lateinit var binding: FragmentTaskListBinding
    private lateinit var taskViewModel: TaskViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTaskListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        taskViewModel = ViewModelProvider(requireActivity()).get(TaskViewModel::class.java)

        binding.newTaskButton.setOnClickListener {
            NewTaskSheet(null).show(childFragmentManager, "newTaskTag")
        }

        setRecyclerView()
    }

    private fun setRecyclerView() {
        val fragment = this
        taskViewModel.taskItems.observe(viewLifecycleOwner) {
            binding.todoListRecyclerView.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = TaskItemAdapter(it, fragment)
            }
        }
    }

    override fun editTaskItem(taskItem: TaskItem) {
        NewTaskSheet(taskItem).show(childFragmentManager, "newTaskTag")
    }

    override fun completeTaskItem(taskItem: TaskItem) {
        taskViewModel.setCompleted(taskItem)
    }
}
