package nl.tudelft.trustchain.atomicswap

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.atomicswap.community.TrustChainCommunityWrapper
import nl.tudelft.trustchain.atomicswap.swap.WalletHolder
import nl.tudelft.trustchain.atomicswap.ui.enums.TradeOfferStatus
import nl.tudelft.trustchain.atomicswap.ui.swap.LOG
import nl.tudelft.trustchain.atomicswap.ui.tradeoffers.list.TradeOfferItem
import nl.tudelft.trustchain.atomicswap.ui.tradeoffers.list.TradeOfferItemRenderer
import nl.tudelft.trustchain.common.BaseActivity

class AtomicSwapActivity : BaseActivity() {

    private var _tradeOffersAdapter: ItemAdapter? = null

    override val navigationGraph get() = R.navigation.atomic_swap_navigation_graph
    override val bottomNavigationMenu get() = R.menu.atomic_swap_menu

    private val atomicSwapCommunity = IPv8Android.getInstance().getOverlay<AtomicSwapCommunity>()!!

    val tradeOffersAdapter get() = _tradeOffersAdapter!!

    val model: AtomicSwapViewModel by viewModels {
        AtomicSwapViewModelFactory(atomicSwapCommunity, WalletHolder, TrustChainCommunityWrapper())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAtomicSwapCallbacks()
        initializeTradesAdapter()
    }

    override fun onDestroy() {
        super.onDestroy()
        _tradeOffersAdapter = null
    }

    fun refreshTradeOffers() {
        Log.i(LOG, "Refreshing trades")
        // TODO: implement fetching already created trades
    }

    fun updateTradeOffersAdapter() {
        lifecycleScope.launch(Dispatchers.Main) {
            val openTrades = model.tradeOffers.map { TradeOfferItem.fromTrade(it.first) }
            tradeOffersAdapter.updateItems(openTrades)
        }
    }

    private fun initializeTradesAdapter() {
        _tradeOffersAdapter = ItemAdapter()

        val renderer = TradeOfferItemRenderer(
            context = this,
            acceptCallback = { acceptTrade(it) }
        )
        tradeOffersAdapter.registerRenderer(renderer)
    }

    private fun configureAtomicSwapCallbacks() {

        // BOB RECEIVES A TRADE OFFER AND ACCEPTS IT
        atomicSwapCommunity.setOnTrade { trade, peer ->
            model.receivedTradeMessage(trade, peer)
            updateTradeOffersAdapter()
        }

        // ALICE RECEIVES AN ACCEPT AND CREATES A TRANSACTION THAT CAN BE CLAIMED BY BOB
        atomicSwapCommunity.setOnAccept { accept, peer ->
            model.receivedAcceptMessage(accept, peer)
            updateTradeOffersAdapter()
        }

        WalletHolder.swapTransactionConfidenceListener.setOnTransactionConfirmed { entry ->
            model.transactionInitiatorConfirmed(entry)
        }


        // BOB GETS NOTIFIED WHEN ALICE FINISHES HER TRANSACTION AND CREATES HIS OWN TRANSACTION
        atomicSwapCommunity.setOnInitiate { initiateMessage, peer ->
            model.receivedInitiateMessage(initiateMessage, peer)
        }

        WalletHolder.swapTransactionConfidenceListener.setOnTransactionRecipientConfirmed { entry ->
            model.transactionRecipientConfirmed(entry)
            updateTradeOffersAdapter()
        }


        // ALICE GETS NOTIFIED THAT BOB'S TRANSACTION IS COMPLETE AND CLAIMS HER MONEY
        atomicSwapCommunity.setOnComplete { completeMessage, peer ->
            model.receivedCompleteMessage(completeMessage, peer)
            updateTradeOffersAdapter()
        }


        // BOB GETS NOTIFIED THAT ALICE CLAIMED THE MONEY AND REVEALED THE SECRET -> HE CLAIMS THE MONEY
        WalletHolder.swapTransactionBroadcastListener.setOnSecretRevealed { secret, offerId ->
            model.secretRevealed(secret, offerId)
            updateTradeOffersAdapter()
        }


        // END OF SWAP
        WalletHolder.swapTransactionConfidenceListener.setOnClaimedConfirmed {
            model.claimedTransactionConfirmed(it.offerId)
            updateTradeOffersAdapter()
        }

        atomicSwapCommunity.setOnRemove { removeMessage, _ ->
            try {
                val myTrade = model.trades.find { it.id == removeMessage.offerId.toLong() }
                /* Only remove the trade if you weren't involved in it */
                if (myTrade == null)
                {
                    model.tradeOffers.remove(model.tradeOffers.first { it.first.id == removeMessage.offerId.toLong() })
                    updateTradeOffersAdapter()
                }
            } catch (e: Exception) {
                Log.d(LOG, e.stackTraceToString())
            }
        }
    }

    /* call this when user accepts trade offer from Trade Offers screen */
    private fun acceptTrade(tradeOfferItem: TradeOfferItem) {
        val tradeOffer = model.tradeOffers.find { it.first.id == tradeOfferItem.id }
        if (tradeOffer != null)
        {
            val trade = tradeOffer.first
            val peer = tradeOffer.second

            lifecycleScope.launch(Dispatchers.Main) {
                val alertDialogBuilder = AlertDialog.Builder(this@AtomicSwapActivity)
                alertDialogBuilder.setTitle("Received Trade Offer")
                alertDialogBuilder.setMessage(trade.toString())
                alertDialogBuilder.setPositiveButton("Accept") { _, _ ->

                    trade.status = TradeOfferStatus.IN_PROGRESS
                    updateTradeOffersAdapter()
                    val newTrade = trade.copy()
                    model.trades.add(newTrade)

                    newTrade.setOnTrade()
                    val myPubKey = newTrade.myPubKey ?: error("Some fields are not initialized")
                    val myAddress = newTrade.myAddress ?: error("Some fields are not initialized")
                    atomicSwapCommunity.sendAcceptMessage(
                        peer,
                        trade.id.toString(),
                        myPubKey.toHex(),
                        myAddress
                    )
                    Log.d(LOG, "Bob accepted a trade offer from Alice")
                    Log.d(LOG, "SENDING ACCEPT TO PEER ${peer.mid}")
                }

                alertDialogBuilder.setCancelable(true)
                alertDialogBuilder.show()
            }
        }
    }

}
