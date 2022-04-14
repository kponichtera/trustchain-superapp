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
import org.junit.Assert
import org.junit.Test

class RecipientConfidenceTests {

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
    val myPubKey = "1".toByteArray()
    val myAddress = "1"
    val counterpartyPubKey = "2".toByteArray()
    val secretHash = "2".toByteArray()
    val counterpartyBitcoinTransaction = "3".toByteArray()
    val counterpartyAddress = "2"
    val myBitcoinTransaction = "4".toByteArray()
    val secret = "5".toByteArray()

    val entry = TransactionConfidenceEntry("1","1",peer)

    @Test(expected = NoSuchElementException::class)
    fun `when transaction confirmed but trades are empty throw exception`(){
        viewModel.transactionRecipientConfirmed(entry)
    }

    @Test(expected = IllegalStateException::class)
    fun `transaction confirmed but it is not set`(){
        viewModel.trades.add(trade)
        viewModel.transactionRecipientConfirmed(entry)
    }

    @Test(expected = IllegalStateException::class)
    fun `transaction confirmed but it is not initiated`(){
        viewModel.trades.add(trade)
        trade.setOnTrade()
        viewModel.transactionRecipientConfirmed(entry)
    }

    @Test(expected = IllegalStateException::class)
    fun `transaction confirmed but transaction is not created`(){
        viewModel.trades.add(trade)
        trade.setOnTrade()
        trade.setOnInitiate(counterpartyPubKey, secretHash, counterpartyBitcoinTransaction, counterpartyAddress)
        viewModel.transactionRecipientConfirmed(entry)
    }

    @Test(expected = NoSuchElementException::class)
    fun `transaction confirmed but no offer in tradeoffers`(){
        viewModel.trades.add(trade)
        trade.setOnTrade()
        trade.setOnInitiate(counterpartyPubKey, secretHash, counterpartyBitcoinTransaction, counterpartyAddress)
        trade.setOnTransactionCreated(myBitcoinTransaction)
        viewModel.transactionRecipientConfirmed(entry)
    }

    @Test
    fun `when transaction confirmed the status should be completed`(){
        viewModel.trades.add(trade)
        viewModel.tradeOffers.add(Pair(trade, peer))
        trade.setOnTrade()
        trade.setOnInitiate(counterpartyPubKey, secretHash, counterpartyBitcoinTransaction, counterpartyAddress)
        trade.setOnTransactionCreated(myBitcoinTransaction)
        viewModel.transactionRecipientConfirmed(entry)
        Assert.assertEquals(viewModel.tradeOffers.first().first.status,TradeOfferStatus.COMPLETED)
    }


}
