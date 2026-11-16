package com.inkflow.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.inkflow.app.ui.InkFlowApp
import com.inkflow.app.ui.InkFlowViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as InkFlowApplication
        setContent {
            val inkFlowViewModel: InkFlowViewModel = viewModel(
                factory = InkFlowViewModel.Factory(
                    repository = app.repository,
                    preferencesRepository = app.preferencesRepository
                )
            )
            InkFlowApp(viewModel = inkFlowViewModel)
        }
    }
}
