package nl.tudelft.trustchain.atomicswap

import nl.tudelft.trustchain.atomicswap.swap.Currency
import nl.tudelft.trustchain.atomicswap.swap.Trade
import nl.tudelft.trustchain.atomicswap.ui.enums.TradeOfferStatus
import nl.tudelft.trustchain.atomicswap.viewmodel.InitiatorWalletHolder
import org.junit.Assert
import org.junit.Test

class TradeTest {

    val trade = Trade(InitiatorWalletHolder,1, TradeOfferStatus.OPEN, Currency.BTC,"10", Currency.ETH, "20")
    val myPubKey = "1".toByteArray()
    val myAddress = "1"
    val counterpartyPubKey = "2".toByteArray()
    val secretHash = "2".toByteArray()
    val counterpartyBitcoinTransaction = "3".toByteArray()
    val counterpartyAddress = "2"
    val myBitcoinTransaction = "4".toByteArray()
    val secret = "5".toByteArray()

    @Test
    fun `trade flow for recipient`(){
        trade.setOnTrade()
        Assert.assertArrayEquals(myPubKey, trade.myPubKey)
        Assert.assertEquals(myAddress, trade.myAddress)

        trade.setOnInitiate(counterpartyPubKey, secretHash, counterpartyBitcoinTransaction, counterpartyAddress)
        Assert.assertArrayEquals(counterpartyPubKey, trade.counterpartyPubKey)
        Assert.assertArrayEquals(secretHash, trade.secretHash)
        Assert.assertArrayEquals(counterpartyBitcoinTransaction, trade.counterpartyBitcoinTransaction)
        Assert.assertEquals(counterpartyAddress, trade.counterpartyAddress)

        trade.setOnTransactionCreated(myBitcoinTransaction)
        Assert.assertArrayEquals(myBitcoinTransaction, trade.myBitcoinTransaction)

        trade.setOnSecretObserved(secret)
        Assert.assertEquals(secret, trade.secret)
    }

    @Test
    fun `trade flow for initiator`(){
        trade.setOnAccept(counterpartyPubKey, counterpartyAddress)
        Assert.assertArrayEquals(myPubKey, trade.myPubKey)
        Assert.assertEquals(myAddress, trade.myAddress)
        Assert.assertArrayEquals(counterpartyPubKey, trade.counterpartyPubKey)
        Assert.assertEquals(counterpartyAddress, trade.counterpartyAddress)
        Assert.assertNotNull(trade.secret)
        Assert.assertNotNull(trade.secretHash)

        trade.setOnTransactionCreated(myBitcoinTransaction)
        Assert.assertArrayEquals(myBitcoinTransaction, trade.myBitcoinTransaction)

        trade.setOnComplete(counterpartyBitcoinTransaction)
        Assert.assertArrayEquals(counterpartyBitcoinTransaction, trade.counterpartyBitcoinTransaction)
    }
}
