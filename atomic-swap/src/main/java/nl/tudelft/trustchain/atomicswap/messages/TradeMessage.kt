package nl.tudelft.trustchain.atomicswap.messages

import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.Serializable
import nl.tudelft.ipv8.util.toHex

data class TradeMessage(val offerId: String, val fromCoin: String, val toCoin: String, val fromAmount: String, val toAmount: String) : Serializable {
    override fun serialize(): ByteArray {
        val msgString = "$offerId;$fromCoin;$toCoin;$fromAmount;$toAmount;"
        println("1234 " + msgString.toByteArray().toHex())
        return msgString.toByteArray()
    }

    companion object Deserializer : Deserializable<TradeMessage> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<TradeMessage, Int> {
            val (offerId, fromCoin, toCoin, fromAmount, toAmount) = buffer.drop(offset).toByteArray().decodeToString().split(";")
            return Pair(TradeMessage(offerId, fromCoin, toCoin, fromAmount, toAmount), buffer.size)
        }
    }
}
