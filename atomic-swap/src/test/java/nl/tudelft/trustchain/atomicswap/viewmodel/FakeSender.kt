package nl.tudelft.trustchain.atomicswap.viewmodel

import nl.tudelft.ipv8.Peer
import nl.tudelft.trustchain.atomicswap.MessageSender
import nl.tudelft.trustchain.atomicswap.OnAcceptReturn

class FakeSender : MessageSender {
    override fun sendAcceptMessage(
        peer: Peer,
        offerId: String,
        btcPubKey: String,
        ethAddress: String
    ) {
    }

    override fun sendInitiateMessage(peer: Peer, offerId: String, data: OnAcceptReturn) {}

    override fun sendCompleteMessage(peer: Peer, offerId: String, txId: String) {}

    override fun sendRemoveTradeMessage(offerId: String) {}
}
