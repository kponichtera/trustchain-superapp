package nl.tudelft.trustchain.atomicswap

import android.app.Application
import androidx.lifecycle.ViewModel

import androidx.lifecycle.ViewModelProvider

@Suppress("UNCHECKED_CAST")
class AtomicSwapViewModelFactory(private val sender: MessageSender) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AtomicSwapViewModel::class.java)) {
            return AtomicSwapViewModel(sender) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
