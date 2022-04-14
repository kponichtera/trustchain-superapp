package nl.tudelft.trustchain.atomicswap.viewmodel.mocks

import nl.tudelft.trustchain.atomicswap.BitcoinSwapI
import nl.tudelft.trustchain.atomicswap.TransactionConfidenceEntry
import nl.tudelft.trustchain.atomicswap.TransactionListenerEntry
import nl.tudelft.trustchain.atomicswap.swap.WalletAPI
import nl.tudelft.trustchain.atomicswap.swap.eth.EthereumSwapI
import org.bitcoinj.core.Transaction

object InitiatorWalletHolder: WalletAPI {
    override val bitcoinSwap: BitcoinSwapI
        get() = BitcoinSwapMock()
    override val ethSwap: EthereumSwapI
        get() = EthereumSwapMock()

    override fun getEthAddress(): String {
        return "1"
    }

    override fun getBitcoinPubKey(): ByteArray {
        return "1".toByteArray()
    }

    override fun broadcastBitcoinTransaction(transaction: Transaction) {}

    override fun addInitiatorEntryToConfidenceListener(entry: TransactionConfidenceEntry) {}

    override fun addClaimedEntryToConfidenceListener(entry: TransactionConfidenceEntry) {}

    override fun addRecipientEntryToConfidenceListener(entry: TransactionConfidenceEntry) {}

    override fun addWatchedAddress(entry: TransactionListenerEntry) {}

    override fun commitBitcoinTransaction(transaction: Transaction) {}
}
