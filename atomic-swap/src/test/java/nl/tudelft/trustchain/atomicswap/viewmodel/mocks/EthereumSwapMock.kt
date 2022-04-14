package nl.tudelft.trustchain.atomicswap.viewmodel.mocks

import nl.tudelft.trustchain.atomicswap.swap.Trade
import nl.tudelft.trustchain.atomicswap.swap.eth.AtomicSwapContract
import nl.tudelft.trustchain.atomicswap.swap.eth.ClaimCallback
import nl.tudelft.trustchain.atomicswap.swap.eth.EthereumSwapI
import org.web3j.protocol.core.methods.response.TransactionReceipt
import java.math.BigInteger

class EthereumSwapMock: EthereumSwapI {
    override fun createSwap(trade: Trade): String {
        return "1"
    }

    override fun getSwap(hash: ByteArray): AtomicSwapContract.Swap {
        return AtomicSwapContract.Swap(BigInteger("1"),"1","1",BigInteger("1"))
    }

    override fun setOnClaimed(hash: ByteArray, cb: ClaimCallback) {

    }

    override fun claimSwap(secret: ByteArray): TransactionReceipt {
       return TransactionReceipt()
    }
}
