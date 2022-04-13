package nl.tudelft.trustchain.atomicswap.viewmodel

import nl.tudelft.trustchain.atomicswap.BitcoinSwapI
import nl.tudelft.trustchain.atomicswap.swap.Trade
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.core.Transaction
import org.bitcoinj.params.RegTestParams
import org.bitcoinj.script.Script

class BitcoinSwapMock: BitcoinSwapI {
    override fun createSwapTransaction(trade: Trade): Pair<Transaction, Script> {
        val transaction = Transaction(RegTestParams.get(), "1".toByteArray())
        val script = Script("2".toByteArray())
        return Pair(transaction, script)
    }

    override fun createClaimTransaction(trade: Trade): Transaction {
        return Transaction(RegTestParams.get(), "1".toByteArray())
    }
}
