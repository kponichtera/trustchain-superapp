package nl.tudelft.trustchain.demo

import android.util.Log
import nl.tudelft.ipv8.IPv4Address
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.messaging.*
import nl.tudelft.ipv8.messaging.payload.IntroductionResponsePayload
import java.util.*

class DemoCommunity : Community() {
    override val serviceId = "02313685c1912a141279f8248fc8db5899c5df5a"

    private val BROADCAST_TRADE_MESSAGE_ID = 1
    private val ACCEPT_MESSAGE_ID = 2
    val discoveredAddressesContacted: MutableMap<IPv4Address, Date> = mutableMapOf()
    val lastTrackerResponses = mutableMapOf<IPv4Address, Date>()

    override fun walkTo(address: IPv4Address) {
        super.walkTo(address)

        discoveredAddressesContacted[address] = Date()
    }

    // Retrieve the trustchain community
    private fun getTrustChainCommunity(): TrustChainCommunity {
        return IPv8Android.getInstance().getOverlay()
            ?: throw IllegalStateException("TrustChainCommunity is not configured")
    }

    override fun onIntroductionResponse(peer: Peer, payload: IntroductionResponsePayload) {
        super.onIntroductionResponse(peer, payload)

        if (peer.address in DEFAULT_ADDRESSES) {
            lastTrackerResponses[peer.address] = Date()
        }
    }

    init {
        messageHandlers[BROADCAST_TRADE_MESSAGE_ID] = ::onMessage
        messageHandlers[ACCEPT_MESSAGE_ID] = ::onMessage
    }

    private fun onMessage(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(TradeMessage.Deserializer)
        Log.d("DemoCommunity", peer.mid + ": " + payload.offerId)
        send(peer.address, serializePacket(ACCEPT_MESSAGE_ID, AcceptMessage(payload.offerId, peer.mid)))
    }


    fun broadcastTradeOffer(offerId: Int, amount: Double) {
        for (peer in getPeers()) {
            val packet = serializePacket(BROADCAST_TRADE_MESSAGE_ID, TradeMessage(offerId.toString(),
                                                                    TradeConstants.BITCOIN,
                                                                    TradeConstants.BITCOIN,
                                                                    amount.toString(),
                                                                    amount.toString()))
            send(peer.address, packet)
        }
    }
}
