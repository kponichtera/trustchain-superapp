package nl.tudelft.trustchain.demo

import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.Serializable
import nl.tudelft.ipv8.util.toHex

class AcceptMessage(val offerId: String, val publicKey: String) : Serializable {
    override fun serialize(): ByteArray {
        val msgString = "$offerId;$publicKey;"
        println(msgString.toByteArray().toHex())
        return msgString.toByteArray()
    }

    companion object Deserializer : Deserializable<AcceptMessage> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<AcceptMessage, Int> {
            val (offerId, publicKey) = buffer.decodeToString()
                .split(";")
            return Pair(AcceptMessage(offerId, publicKey), buffer.size)
        }
    }
}
