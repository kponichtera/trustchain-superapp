package nl.tudelft.trustchain.atomicswap

import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.keyvault.Key
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.trustchain.atomicswap.messages.TradeMessage
import nl.tudelft.trustchain.atomicswap.swap.Currency
import nl.tudelft.trustchain.atomicswap.swap.Trade
import nl.tudelft.trustchain.atomicswap.ui.enums.TradeOfferStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class FakeSender: MessageSender{
    override fun sendAcceptMessage(
        peer: Peer,
        offerId: String,
        btcPubKey: String,
        ethAddress: String
    ) {
        TODO("Not yet implemented")
    }

    override fun sendInitiateMessage(peer: Peer, offerId: String, data: OnAcceptReturn) {
        TODO("Not yet implemented")
    }

    override fun sendCompleteMessage(peer: Peer, offerId: String, txId: String) {
        TODO("Not yet implemented")
    }

    override fun sendRemoveTradeMessage(offerId: String) {
        TODO("Not yet implemented")
    }

}

class AtomicSwapViewModelTest() {

    val viewModel = AtomicSwapViewModel(FakeSender())
    val peer: Peer = Peer(FakeKey())

    @Test
    fun `when recevedTradeMessage() is called, should add trade to trade offers`() {

        val tradeMessage: TradeMessage = TradeMessage("1", "ETH", "BTC", "20","10")
        val trade = Trade(
            tradeMessage.offerId.toLong(),
            TradeOfferStatus.OPEN,
            Currency.fromString(tradeMessage.toCoin),
            tradeMessage.toAmount,
            Currency.fromString(tradeMessage.fromCoin),
            tradeMessage.fromAmount
        )

        viewModel.receivedTradeMessage(tradeMessage, peer)
        assertEquals(trade, viewModel.tradeOffers[0].first)
    }
}

class FakeKey:Key{
    override fun pub(): PublicKey {
        TODO("Not yet implemented")
    }

    override fun keyToBin(): ByteArray {
        TODO("Not yet implemented")
    }

}
