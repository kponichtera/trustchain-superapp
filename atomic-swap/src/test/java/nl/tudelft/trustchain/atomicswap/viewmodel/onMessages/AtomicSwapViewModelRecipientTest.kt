package nl.tudelft.trustchain.atomicswap.viewmodel.onMessages

import nl.tudelft.ipv8.Peer
import nl.tudelft.trustchain.atomicswap.AtomicSwapViewModel
import nl.tudelft.trustchain.atomicswap.messages.TradeMessage
import nl.tudelft.trustchain.atomicswap.swap.Currency
import nl.tudelft.trustchain.atomicswap.swap.Trade
import nl.tudelft.trustchain.atomicswap.ui.enums.TradeOfferStatus
import nl.tudelft.trustchain.atomicswap.viewmodel.mocks.FakeKey
import nl.tudelft.trustchain.atomicswap.viewmodel.mocks.FakeSender
import nl.tudelft.trustchain.atomicswap.viewmodel.mocks.RecipientWalletHolder
import nl.tudelft.trustchain.atomicswap.viewmodel.mocks.TrustChainWrapperMock
import org.junit.Assert
import org.junit.Test

class AtomicSwapViewModelRecipientTest() {

    val viewModel = AtomicSwapViewModel(FakeSender(), RecipientWalletHolder, TrustChainWrapperMock())
    val peer: Peer = Peer(FakeKey())
    val trade = Trade(RecipientWalletHolder,1, TradeOfferStatus.OPEN, Currency.ETH,"20", Currency.BTC, "10")

    val myPubKey = "1".toByteArray()
    val myAddress = "1"
    val counterpartyPubKey = "2".toByteArray()
    val secretHash = "2".toByteArray()
    val counterpartyBitcoinTransaction = "3".toByteArray()
    val counterpartyAddress = "2"
    val myBitcoinTransaction = "4".toByteArray()
    val secret = "5".toByteArray()

    @Test
    fun `when receivedTradeMessage() is called, should add trade to trade offers`() {
        val tradeMessage = TradeMessage("1", "BTC", "ETH", "10","20")
        viewModel.receivedTradeMessage(tradeMessage, peer)
        Assert.assertEquals(trade, viewModel.tradeOffers[0].first)
    }
}
