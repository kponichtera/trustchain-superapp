package nl.tudelft.trustchain.demo

import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.Serializable
import nl.tudelft.ipv8.util.toHex

object TradeConstants {
    const val BITCOIN = "BTC"
    const val ETHEREUM = "ETH"
}

class TradeMessage(val offerId: String, val fromCoin: String, val toCoin: String, val fromAmount: String, val toAmount: String) : Serializable {
    override fun serialize(): ByteArray {
        val msgString = "$offerId;$fromCoin;$toCoin$fromAmount;$toAmount;"
        println(msgString.toByteArray().toHex())
        return msgString.toByteArray()
    }

    companion object Deserializer : Deserializable<TradeMessage> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<TradeMessage, Int> {
            val (offerId, fromCoin, toCoin, fromAmount, toAmount) = buffer.decodeToString().split(";")
            return Pair(TradeMessage(offerId, fromCoin, toCoin, fromAmount, toAmount), buffer.size)
        }
    }
}
