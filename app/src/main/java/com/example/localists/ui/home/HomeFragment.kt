package com.example.localists.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.localists.TaskViewModel
import com.example.localists.TaskDetailsFragment
import com.example.localists.databinding.FragmentHomeBinding
import com.example.localists.R

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var taskViewModel: TaskViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        taskViewModel = ViewModelProvider(requireActivity()).get(TaskViewModel::class.java)
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        taskViewModel.loadTaskItems(requireContext())

        val listView = binding.navHostFragmentContentMain
        taskViewModel.taskItems.observe(viewLifecycleOwner) { taskItems ->
            Log.d("HomeFragment", "Task items updated: ${taskItems.map { it.name }}")
            val adapter = ArrayAdapter(
                requireContext(), android.R.layout.simple_list_item_1, taskItems
            )
            listView.adapter = adapter
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            try {
                val taskItems = taskViewModel.taskItems.value
                if (taskItems != null && position < taskItems.size) {
                    val taskItem = taskItems[position]
                    val taskDetailsFragment = TaskDetailsFragment()
                    val args = Bundle().apply {
                        putSerializable("taskId", taskItem.id)
                    }
                    taskDetailsFragment.arguments = args

                    requireActivity().supportFragmentManager.beginTransaction()
                        .replace(R.id.nav_host_fragment_content_main, taskDetailsFragment)
                        .addToBackStack(null)
                        .commit()
                    Log.d("HomeFragment", "Navigated to TaskDetailsFragment for task: ${taskItem.name}")
                } else {
                    Log.e("HomeFragment", "Task items null or position out of bounds: $position")
                }
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error navigating to TaskDetailsFragment: ${e.message}")
            }
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}