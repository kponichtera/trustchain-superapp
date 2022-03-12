package nl.tudelft.trustchain.atomicswap.swap

import junit.framework.TestCase
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import nl.tudelft.trustchain.atomicswap.BitcoinSwap
import org.bitcoinj.core.*
import org.bitcoinj.params.UnitTestParams
import org.bitcoinj.script.Script
import org.bitcoinj.script.ScriptBuilder
import org.bitcoinj.store.MemoryBlockStore
import org.bitcoinj.wallet.Wallet
import org.junit.Test

class BitcoinSwapTest {
    init {
        initBitcoinSwap()
    }

    fun initBitcoinSwap() {
        BitcoinSwap(relativeLock = 1,networkParams = UnitTestParams())
    }

    fun createWallet(): Wallet {
        return Wallet.createDeterministic(Context(UnitTestParams()), Script.ScriptType.P2PKH)
    }

//    fun fundWallet(wallet: Wallet,amount:Coin){
//        val blockStore =  MemoryBlockStore(UnitTestParams());
//
//        val chain = BlockChain(Context(UnitTestParams()),wallet,blockStore)
//
//    }

    @Test
    fun `A swap transaction should be able to be claimed`() {
        // the wallet that can reclaim
        val initiateWallet = createWallet()

        // the wallet that can claim
        val claimWallet = createWallet()
        val claimPublicKey = claimWallet.freshReceiveKey().pubKey

        val fakeTx = Transaction(UnitTestParams())
        fakeTx.addInput(Sha256Hash.ZERO_HASH,0,ScriptBuilder.createEmpty())
        fakeTx.addOutput(Coin.parseCoin("100"),initiateWallet.currentReceiveAddress())

        initiateWallet.commitTx(fakeTx)
        initiateWallet.allowSpendingUnconfirmedTransactions() // allow us to use unconfirmed txs

        val (tx, swapData) = BitcoinSwap.startSwapTx(
            offerId = 0,
            wallet = initiateWallet,
            claimPubKey = claimPublicKey,
            "1" //the wallet has no funds. todo: Figure out how to mock balance (?).
        )



        val claimTx = BitcoinSwap.createClaimTx(tx.txId.bytes, swapData.secretUsed, 0, claimWallet)

        val result = runCatching {
            claimTx.inputs.first().verify(tx.outputs.find { it.scriptPubKey.scriptType == Script.ScriptType.P2SH })
        }

        assertTrue("Swap Tx should be claimable",result.isSuccess)

    }
}
