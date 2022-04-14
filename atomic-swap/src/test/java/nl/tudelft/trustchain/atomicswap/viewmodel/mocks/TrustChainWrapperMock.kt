package nl.tudelft.trustchain.atomicswap.viewmodel.mocks

import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.attestation.trustchain.TrustChainTransaction
import nl.tudelft.trustchain.atomicswap.community.TrustChainWrapperAPI

class TrustChainWrapperMock: TrustChainWrapperAPI {
    override fun createProposalBlock(tchain_trans: TrustChainTransaction, peer: Peer) {}
}
