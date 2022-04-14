package nl.tudelft.trustchain.atomicswap

import android.app.Application
import androidx.lifecycle.ViewModel

import androidx.lifecycle.ViewModelProvider
import nl.tudelft.trustchain.atomicswap.community.TrustChainWrapperAPI
import nl.tudelft.trustchain.atomicswap.swap.WalletAPI

@Suppress("UNCHECKED_CAST")
class AtomicSwapViewModelFactory(private val sender: MessageSender, private val walletAPI: WalletAPI, private val trustChainWrapperAPI: TrustChainWrapperAPI) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AtomicSwapViewModel::class.java)) {
            return AtomicSwapViewModel(sender, walletAPI, trustChainWrapperAPI) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
