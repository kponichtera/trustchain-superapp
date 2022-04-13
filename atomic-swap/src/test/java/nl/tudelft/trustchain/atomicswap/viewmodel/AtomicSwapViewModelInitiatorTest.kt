package nl.tudelft.trustchain.atomicswap

import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.atomicswap.messages.AcceptMessage
import nl.tudelft.trustchain.atomicswap.swap.Currency
import nl.tudelft.trustchain.atomicswap.swap.Trade
import nl.tudelft.trustchain.atomicswap.ui.enums.TradeOfferStatus
import nl.tudelft.trustchain.atomicswap.viewmodel.FakeKey
import nl.tudelft.trustchain.atomicswap.viewmodel.FakeSender
import nl.tudelft.trustchain.atomicswap.viewmodel.InitiatorWalletHolder
import org.junit.Assert.assertEquals
import org.junit.Test

class AtomicSwapViewModelInitiatorTestTest {

    val viewModel = AtomicSwapViewModel(FakeSender(), InitiatorWalletHolder)
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

    val myPubKey = "1".toByteArray()
    val myAddress = "1"

    val counterpartyPubKey = "2".toByteArray()
    val counterpartyAddress = "2"

    val secretHash = "2".toByteArray()
    val counterpartyBitcoinTransaction = "3".toByteArray()
    val myBitcoinTransaction = "4".toByteArray()
    val secret = "5".toByteArray()

    @Test(expected = NoSuchElementException::class)
    fun `when received accept message and empty tradeOffers and trades throw exception`() {
        val acceptMessage = AcceptMessage("1", counterpartyPubKey.toHex(), counterpartyAddress)
        viewModel.receivedAcceptMessage(acceptMessage, peer)
    }

    @Test(expected = NoSuchElementException::class)
    fun `when received accept message and empty tradeOffers but not trades throw exception`() {
        val acceptMessage = AcceptMessage("1", counterpartyPubKey.toHex(), counterpartyAddress)
        viewModel.trades.add(trade)
        viewModel.receivedAcceptMessage(acceptMessage, peer)
    }

    @Test
    fun `when received accept change status of trade offer`() {
        val acceptMessage = AcceptMessage("1", counterpartyPubKey.toHex(), counterpartyAddress)
        viewModel.trades.add(trade)
        viewModel.tradeOffers.add(Pair(trade, peer))
        viewModel.receivedAcceptMessage(acceptMessage, peer)
        assertEquals(TradeOfferStatus.IN_PROGRESS, viewModel.tradeOffers.first().first.status)
    }

    @Test(expected = NoSuchElementException::class)
    fun `transaction confirmed but not in trades list`(){
        val entry = TransactionConfidenceEntry("1","1",peer)
        viewModel.transactionConfirmed(entry)
    }

    @Test(expected = IllegalStateException::class)
    fun `transaction confirmed but it is not accepted`(){
        viewModel.trades.add(trade)
        val entry = TransactionConfidenceEntry("1","1",peer)
        viewModel.transactionConfirmed(entry)
    }

    @Test
    fun `transaction confirmed`(){
        viewModel.trades.add(trade)
        trade.setOnAccept(counterpartyPubKey,counterpartyAddress)
        trade.setOnTransactionCreated("1".toByteArray())
        val entry = TransactionConfidenceEntry("1","1",peer)
        viewModel.transactionConfirmed(entry)
    }
}
