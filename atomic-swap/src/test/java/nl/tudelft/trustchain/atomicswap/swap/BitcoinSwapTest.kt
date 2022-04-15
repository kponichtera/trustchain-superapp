package nl.tudelft.trustchain.atomicswap.swap

import junit.framework.TestCase.assertTrue
import nl.tudelft.trustchain.atomicswap.BitcoinSwap
import nl.tudelft.trustchain.atomicswap.BitcoinSwapI
import nl.tudelft.trustchain.atomicswap.TransactionConfidenceEntry
import nl.tudelft.trustchain.atomicswap.TransactionListenerEntry
import nl.tudelft.trustchain.atomicswap.swap.eth.EthereumSwapI
import nl.tudelft.trustchain.atomicswap.ui.enums.TradeOfferStatus
import org.bitcoinj.core.*
import org.bitcoinj.params.UnitTestParams
import org.bitcoinj.script.Script
import org.bitcoinj.script.ScriptBuilder
import org.bitcoinj.wallet.Wallet
import org.junit.Test

class BitcoinSwapTest {


    private fun createBitcoinSwap(): BitcoinSwap {
        return BitcoinSwap(relativeLock = 1,networkParams = UnitTestParams())
    }

    private fun createWallet(): Wallet {
        return Wallet.createDeterministic(Context(UnitTestParams()), Script.ScriptType.P2PKH)
    }

//    fun fundWallet(wallet: WalletHolder,amount:Coin){
//        val blockStore =  MemoryBlockStore(UnitTestParams());
//
//        val chain = BlockChain(Context(UnitTestParams()),wallet,blockStore)
//
//    }
    /**
     * Creates a fake transaction that pays [amount] coins to the wallet and sets the wallet to
     * allow spending unconfirmed outputs.
     *
     * This is usefully for tests that need the wallets to have funds.
     */
    fun fundWallet(wallet: Wallet,amount:Coin){
        val fakeTx = Transaction(UnitTestParams())
        fakeTx.addInput(Sha256Hash.ZERO_HASH,0,ScriptBuilder.createEmpty())
        fakeTx.addOutput(amount,wallet.currentReceiveAddress())

        wallet.commitTx(fakeTx)
        wallet.allowSpendingUnconfirmedTransactions() // allow us to use unconfirmed txs
    }

    @Test
    fun `A swap transaction should be able to be claimed`() {
        // the wallet that can reclaim
        val initiateWallet = createWallet()
        val initiateBitcoinSwap = createBitcoinSwap()
        val initiatePublicKey = initiateWallet.freshReceiveKey().pubKey
        fundWallet(initiateWallet,Coin.parseCoin("10"))

        // the wallet that can claim
        val claimWallet = createWallet()
        val claimPublicKey = claimWallet.freshReceiveKey().pubKey
        val claimBitcoinSwap = createBitcoinSwap()

        val initiateWalletApi: WalletAPI = object :WalletAPI{
            override val bitcoinSwap: BitcoinSwapI
                get() = error("")
            override val ethSwap: EthereumSwapI
                get() = error("")

            override fun getEthAddress(): String = ""

            override fun getBitcoinPubKey(): ByteArray = initiatePublicKey

            override fun broadcastBitcoinTransaction(transaction: Transaction) = error("")

            override fun addInitiatorEntryToConfidenceListener(entry: TransactionConfidenceEntry) = error("")

            override fun addClaimedEntryToConfidenceListener(entry: TransactionConfidenceEntry) = error("")

            override fun addRecipientEntryToConfidenceListener(entry: TransactionConfidenceEntry) = error("")
            override fun addWatchedAddress(entry: TransactionListenerEntry) = error("")

            override fun commitBitcoinTransaction(transaction: Transaction) = error("")

        }

        val initiateTrade = Trade(initiateWalletApi,0,TradeOfferStatus.IN_PROGRESS,Currency.BTC,"1",Currency.BTC,"1")

        initiateTrade.setOnAccept(claimPublicKey,"")

        val (tx, _) = initiateBitcoinSwap.createSwapTransaction(
            initiateTrade,
            initiateWallet
        )

        claimWallet.commitTx(tx)



        val claimWalletApi: WalletAPI = object : WalletAPI{
            override val bitcoinSwap: BitcoinSwapI
                get() = error("")
            override val ethSwap: EthereumSwapI
                get() = error("")

            override fun getEthAddress(): String = ""

            override fun getBitcoinPubKey(): ByteArray = claimPublicKey

            override fun broadcastBitcoinTransaction(transaction: Transaction) = error("")

            override fun addInitiatorEntryToConfidenceListener(entry: TransactionConfidenceEntry) = error("")

            override fun addClaimedEntryToConfidenceListener(entry: TransactionConfidenceEntry) = error("")

            override fun addRecipientEntryToConfidenceListener(entry: TransactionConfidenceEntry) = error("")
            override fun addWatchedAddress(entry: TransactionListenerEntry) = error("")

            override fun commitBitcoinTransaction(transaction: Transaction) = error("")

        }

        val claimTrade = Trade(claimWalletApi,0,TradeOfferStatus.IN_PROGRESS,Currency.BTC,"1",Currency.BTC,"1")
        claimTrade.setOnTrade()
        claimTrade.setOnInitiate(initiatePublicKey,initiateTrade.secretHash!!,tx.bitcoinSerialize(),"")
        claimTrade.setOnSecretObserved(initiateTrade.secret!!)
        val claimTx = claimBitcoinSwap.createClaimTransaction(claimTrade,claimWallet)

        val result = runCatching {
            claimTx.inputs.first().verify(tx.outputs.find { it.scriptPubKey.scriptType == Script.ScriptType.P2SH })
        }


        assertTrue("Swap Tx should be claimable",result.isSuccess)

    }

}
