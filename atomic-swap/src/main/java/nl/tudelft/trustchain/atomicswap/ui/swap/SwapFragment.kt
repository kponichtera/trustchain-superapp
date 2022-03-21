package nl.tudelft.trustchain.atomicswap.ui.swap

import android.os.Bundle
import android.util.Log
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.android.synthetic.main.fragment_peers.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.BlockSigner
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.ipv8.attestation.trustchain.validation.TransactionValidator
import nl.tudelft.ipv8.attestation.trustchain.validation.ValidationResult
import nl.tudelft.trustchain.atomicswap.AtomicSwapCommunity
import nl.tudelft.trustchain.atomicswap.R
import nl.tudelft.trustchain.atomicswap.ui.peers.AddressItem
import nl.tudelft.trustchain.atomicswap.ui.peers.PeerItem
import nl.tudelft.trustchain.common.ui.BaseFragment

class SwapFragment : BaseFragment(R.layout.fragment_peers) {
    private val adapter = ItemAdapter()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//
//        setContentView(R.layout.fragment_peers)
//
//        adapter.registerRenderer(PeerItemRenderer {
//            // NOOP
//        })
//
//        adapter.registerRenderer(AddressItemRenderer {
//            // NOOP
//        })
//
//        recyclerView.adapter = adapter
//        recyclerView.layoutManager = LinearLayoutManager(this)
//        recyclerView.addItemDecoration(DividerItemDecoration(this, LinearLayout.VERTICAL))
//
        loadNetworkInfo()
        //receiveGossips()
    }

    private fun loadNetworkInfo() {
        lifecycleScope.launchWhenStarted {
            val atomicSwapCommunity = IPv8Android.getInstance().getOverlay<AtomicSwapCommunity>()!!
            val trustChainCommunity = IPv8Android.getInstance().getOverlay<TrustChainCommunity>()!!
            while (isActive) {
                val peers = atomicSwapCommunity.getPeers()

                val discoveredAddresses = atomicSwapCommunity.network
                    .getWalkableAddresses(atomicSwapCommunity.serviceId)

                val discoveredBluetoothAddresses = atomicSwapCommunity.network
                    .getNewBluetoothPeerCandidates()
                    .map { it.address }

                val peerItems = peers.map {
                    PeerItem(
                        it
                    )
                }

                val addressItems = discoveredAddresses.map { address ->
                    val contacted = atomicSwapCommunity.discoveredAddressesContacted[address]
                    AddressItem(
                        address,
                        null,
                        contacted
                    )
                }

                val bluetoothAddressItems = discoveredBluetoothAddresses.map { address ->
                    AddressItem(
                        address,
                        null,
                        null
                    )
                }

                val items = peerItems + bluetoothAddressItems + addressItems

                for (peer in peers) {
                    Log.d("AtomicSwapCommunity", "FOUND PEER with id " + peer.mid)
                    val publicKey = peer.publicKey.keyToBin()
                    val transaction = mapOf("from_pub_key" to trustChainCommunity.myPeer.mid,
                                            "to_pub_key" to peer.mid)
                    println("trustchain peer PUB KEY " + trustChainCommunity.myPeer.publicKey.toString() + " atomicswap pub key " + atomicSwapCommunity.myPeer.publicKey.toString())
                    trustChainCommunity.createProposalBlock(ATOMIC_SWAP_BLOCK, transaction, publicKey)
                }


                adapter.updateItems(items)
                txtCommunityName.text = atomicSwapCommunity.javaClass.simpleName
                txtPeerCount.text = "${peers.size} peers"
                val textColorResId = if (peers.isNotEmpty()) R.color.green else R.color.red
                val textColor = ResourcesCompat.getColor(resources, textColorResId, null)
                txtPeerCount.setTextColor(textColor)
                imgEmpty.isVisible = items.isEmpty()
                //atomicSwapCommunity.broadcastTradeOffer(1, 0.5)

                delay(5000)
            }
        }
    }

    companion object {
        const val ATOMIC_SWAP_BLOCK = "atomic_swap_block"
    }
}
