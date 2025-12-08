package com.example.localists

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider // NEW: For TaskViewModel
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout // NEW: For swipe down

class ItemFragment : Fragment() {

    private var columnCount = 1
    private lateinit var taskViewModel: TaskViewModel // NEW: To observe tasks
    private lateinit var adapter: MyItemRecyclerViewAdapter // NEW: Adapter reference for updates

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            columnCount = it.getInt(ARG_COLUMN_COUNT)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_item_list, container, false)

        // NEW: Get ViewModel (activity-scoped)
        taskViewModel = ViewModelProvider(requireActivity()).get(TaskViewModel::class.java)

        // Set up RecyclerView
        val recyclerView = view.findViewById<RecyclerView>(R.id.list) // Assume ID from layout
        with(recyclerView) {
            layoutManager = when {
                columnCount <= 1 -> LinearLayoutManager(context)
                else -> GridLayoutManager(context, columnCount)
            }
            adapter = MyItemRecyclerViewAdapter(mutableListOf()) // NEW: Start with empty TaskItem list
            this.adapter = adapter
        }

        // NEW: Set up SwipeRefreshLayout (assume ID 'swipe_refresh' in layout)
        val swipeRefresh = view.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh)
        swipeRefresh.setOnRefreshListener {
            NewTaskSheet(null).show(childFragmentManager, "new_task_tag") // Trigger new task sheet
            swipeRefresh.isRefreshing = false // Stop spinner immediately
        }

        // NEW: Observe tasks and update adapter
        taskViewModel.taskItems.observe(viewLifecycleOwner) { tasks ->
            adapter.updateTasks(tasks ?: emptyList()) // Update with real tasks
        }

        return view
    }

    companion object {
        const val ARG_COLUMN_COUNT = "column-count"

        @JvmStatic
        fun newInstance(columnCount: Int) =
            ItemFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_COLUMN_COUNT, columnCount)
                }
            }
    }
}