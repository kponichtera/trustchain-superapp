package nl.tudelft.trustchain.atomicswap.swap

import android.renderscript.Script
import nl.tudelft.trustchain.atomicswap.*
import nl.tudelft.trustchain.atomicswap.swap.eth.EthereumSwap
import nl.tudelft.trustchain.atomicswap.swap.eth.EthereumSwapI
import nl.tudelft.trustchain.common.bitcoin.WalletService
import nl.tudelft.trustchain.common.ethereum.EthereumWeb3jWallet
import org.bitcoinj.core.Transaction
import org.bitcoinj.kits.WalletAppKit
import org.bitcoinj.wallet.KeyChain

interface WalletAPI{
    val bitcoinSwap: BitcoinSwapI
    val ethSwap: EthereumSwapI
    fun getEthAddress():String
    fun getBitcoinPubKey(): ByteArray
    fun broadcastBitcoinTransaction(transaction: Transaction)
    fun addInitiatorEntryToConfidenceListener(entry: TransactionConfidenceEntry)
    fun addClaimedEntryToConfidenceListener(entry: TransactionConfidenceEntry)
    fun addRecipientEntryToConfidenceListener(entry: TransactionConfidenceEntry)
    fun addWatchedAddress(entry: TransactionListenerEntry)
    fun commitBitcoinTransaction(transaction: Transaction)
}

object WalletHolder: WalletAPI {
    val walletAppKit = WalletService.getGlobalWallet()
    val bitcoinWallet = walletAppKit.wallet()
    val swapTransactionConfidenceListener = SwapTransactionConfidenceListener(6)
    val swapTransactionBroadcastListener = SwapTransactionBroadcastListener()

    lateinit var ethereumWallet: EthereumWeb3jWallet
    override lateinit var ethSwap : EthereumSwap

    override fun getEthAddress(): String {
        return ethereumWallet.address()
    }

    override fun getBitcoinPubKey(): ByteArray {
        return bitcoinWallet.freshKey(KeyChain.KeyPurpose.AUTHENTICATION).pubKey
    }

    override fun broadcastBitcoinTransaction(transaction: Transaction) {
       walletAppKit.peerGroup().broadcastTransaction(transaction)
    }

    override fun addInitiatorEntryToConfidenceListener(entry: TransactionConfidenceEntry) {
        swapTransactionConfidenceListener.addTransactionInitiator(entry)
    }

    override fun addClaimedEntryToConfidenceListener(entry: TransactionConfidenceEntry) {
        swapTransactionConfidenceListener.addTransactionClaimed(entry)
    }

    override fun addRecipientEntryToConfidenceListener(entry: TransactionConfidenceEntry) {
        swapTransactionConfidenceListener.addTransactionRecipient(entry)
    }

    override fun addWatchedAddress(entry: TransactionListenerEntry) {
        swapTransactionBroadcastListener.addWatchedAddress(entry)
    }

    override fun commitBitcoinTransaction(transaction: Transaction) {
        bitcoinWallet.commitTx(transaction)
    }

    init {
        bitcoinWallet.addTransactionConfidenceEventListener(swapTransactionConfidenceListener)
        walletAppKit.peerGroup().addOnTransactionBroadcastListener(swapTransactionBroadcastListener)
    }

    override val bitcoinSwap: BitcoinSwapI = BitcoinSwap()
}
