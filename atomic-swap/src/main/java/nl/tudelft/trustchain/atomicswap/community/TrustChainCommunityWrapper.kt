package nl.tudelft.trustchain.atomicswap.community

import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.attestation.trustchain.TrustChainTransaction
import nl.tudelft.trustchain.atomicswap.AtomicSwapTrustchainConstants

interface TrustChainWrapperAPI{
    fun createProposalBlock(tchain_trans: TrustChainTransaction, peer: Peer)
}

class TrustChainCommunityWrapper: TrustChainWrapperAPI {
    val trustChainCommunity =
        IPv8Android.getInstance().getOverlay<TrustChainCommunity>()!!

    override fun createProposalBlock(tchain_trans: TrustChainTransaction, peer: Peer){

        val publicKey = peer.publicKey.keyToBin()

        trustChainCommunity.createProposalBlock(
            AtomicSwapTrustchainConstants.ATOMIC_SWAP_COMPLETED_BLOCK,
            tchain_trans,
            publicKey
        )
    }
}
