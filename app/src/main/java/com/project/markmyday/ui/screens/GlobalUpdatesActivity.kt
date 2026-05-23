package com.project.markmyday.ui.screens

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.project.markmyday.databinding.ActivityGlobalUpdatesBinding
import com.project.markmyday.viewmodel.GlobalUpdatesViewModel
import com.project.markmyday.viewmodel.NewsState
import com.project.markmyday.ui.adapter.NewsAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class GlobalUpdatesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGlobalUpdatesBinding
    private lateinit var viewModel: GlobalUpdatesViewModel
    private lateinit var newsAdapter: NewsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGlobalUpdatesBinding.inflate(layoutInflater)
        
        // Use the default background color from the theme
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupViewModel()
        setupSwipeRefresh()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        newsAdapter = NewsAdapter()
        binding.rvNews.apply {
            layoutManager = LinearLayoutManager(this@GlobalUpdatesActivity)
            adapter = newsAdapter
        }
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[GlobalUpdatesViewModel::class.java]

        lifecycleScope.launch {
            viewModel.newsState.collectLatest { state ->
                when (state) {
                    is NewsState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.swipeRefreshLayout.isRefreshing = true
                    }
                    is NewsState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        binding.swipeRefreshLayout.isRefreshing = false
                        newsAdapter.submitList(state.articles)
                    }
                    is NewsState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.swipeRefreshLayout.isRefreshing = false
                        Toast.makeText(this@GlobalUpdatesActivity, state.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.fetchNews()
        }
    }
}
