package com.example.localists.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.localists.databinding.FragmentHomeBinding
import com.example.localists.TaskViewModel
import com.example.localists.TaskDetailsFragment
import android.util.Log
import com.example.localists.R


class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var taskViewModel: TaskViewModel // Declare taskViewModel instance

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Initialize the TaskViewModel instance
        taskViewModel = ViewModelProvider(requireActivity()).get(TaskViewModel::class.java)
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Load task items when the fragment is created
        taskViewModel.loadTaskItems(requireContext())

        // Get the ListView reference using the binding
        val listView = binding.navHostFragmentContentMain

        // Set up an observer for the taskItems LiveData
        taskViewModel.taskItems.observe(viewLifecycleOwner) { taskItems ->
            // Update the ListView with the new taskItems
            Log.d("HomeFragment", "Task items updated: $taskItems")
            val adapter = ArrayAdapter(
                requireContext(), android.R.layout.simple_list_item_1, taskItems
            )
            listView.adapter = adapter
        }

        listView.setOnItemClickListener { adapterView, view, i, l ->
            // Handle item click
            val taskItems = taskViewModel.taskItems.value
            if (taskItems != null && i < taskItems.size) {
                val taskItem = taskItems[i]

                // Create a new instance of TaskDetailsFragment
                val taskDetailsFragment = TaskDetailsFragment()

                // Pass the task ID to TaskDetailsFragment
                val args = Bundle()
                args.putSerializable("taskId", taskItem.id)
                taskDetailsFragment.arguments = args

                // Replace the current fragment with TaskDetailsFragment
                val transaction = requireActivity().supportFragmentManager.beginTransaction()
                transaction.replace(R.id.nav_host_fragment_content_main, taskDetailsFragment)
                transaction.addToBackStack(null)
                transaction.commit()
            }
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
