package nl.tudelft.trustchain.atomicswap.viewmodel.onConfidence

import nl.tudelft.ipv8.Peer
import nl.tudelft.trustchain.atomicswap.AtomicSwapViewModel
import nl.tudelft.trustchain.atomicswap.TransactionConfidenceEntry
import nl.tudelft.trustchain.atomicswap.swap.Currency
import nl.tudelft.trustchain.atomicswap.swap.Trade
import nl.tudelft.trustchain.atomicswap.ui.enums.TradeOfferStatus
import nl.tudelft.trustchain.atomicswap.viewmodel.mocks.FakeKey
import nl.tudelft.trustchain.atomicswap.viewmodel.mocks.FakeSender
import nl.tudelft.trustchain.atomicswap.viewmodel.mocks.InitiatorWalletHolder
import nl.tudelft.trustchain.atomicswap.viewmodel.mocks.TrustChainWrapperMock
import org.junit.Test

class InitiatorConfidenceTests {

    val viewModel = AtomicSwapViewModel(FakeSender(), InitiatorWalletHolder, TrustChainWrapperMock())
    val peer: Peer = Peer(FakeKey())
    val trade = Trade(
        InitiatorWalletHolder,
        1,
        TradeOfferStatus.OPEN,
        Currency.BTC,
        "10",
        Currency.ETH,
        "20"
    )
    val counterpartyPubKey = "2".toByteArray()
    val counterpartyAddress = "2"

    val entry = TransactionConfidenceEntry("1","1",peer)

    @Test(expected = NoSuchElementException::class)
    fun `when transaction confirmed but trades are empty throw exception`(){
        viewModel.transactionInitiatorConfirmed(entry)
    }

    @Test(expected = IllegalStateException::class)
    fun `transaction confirmed but it is not accepted`(){
        viewModel.trades.add(trade)
        viewModel.transactionInitiatorConfirmed(entry)
    }

    @Test
    fun `transaction confirmed and is accepted`(){
        viewModel.trades.add(trade)
        trade.setOnAccept(counterpartyPubKey,counterpartyAddress)
        trade.setOnTransactionCreated("1".toByteArray())
        val entry = TransactionConfidenceEntry("1","1",peer)
        viewModel.transactionInitiatorConfirmed(entry)
    }


}
