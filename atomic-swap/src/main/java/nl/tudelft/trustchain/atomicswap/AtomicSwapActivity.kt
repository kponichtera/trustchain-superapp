package nl.tudelft.trustchain.atomicswap

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.trustchain.atomicswap.community.TrustChainCommunityWrapper
import nl.tudelft.trustchain.atomicswap.swap.WalletHolder
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
            try {
                model.receivedTradeMessage(trade, peer)
                updateTradeOffersAdapter()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // ALICE RECEIVES AN ACCEPT AND CREATES A TRANSACTION THAT CAN BE CLAIMED BY BOB
        atomicSwapCommunity.setOnAccept { accept, peer ->
            try {
                model.receivedAcceptMessage(accept, peer)
                updateTradeOffersAdapter()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        WalletHolder.swapTransactionConfidenceListener.setOnTransactionConfirmed { entry ->
            try {
                model.transactionInitiatorConfirmed(entry)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }


        // BOB GETS NOTIFIED WHEN ALICE FINISHES HER TRANSACTION AND CREATES HIS OWN TRANSACTION
        atomicSwapCommunity.setOnInitiate { initiateMessage, peer ->
            try {
                model.receivedInitiateMessage(initiateMessage, peer)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        WalletHolder.swapTransactionConfidenceListener.setOnTransactionRecipientConfirmed { entry ->
            try {
                model.transactionRecipientConfirmed(entry)
                updateTradeOffersAdapter()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }


        // ALICE GETS NOTIFIED THAT BOB'S TRANSACTION IS COMPLETE AND CLAIMS HER MONEY
        atomicSwapCommunity.setOnComplete { completeMessage, peer ->
            try {
                model.receivedCompleteMessage(completeMessage, peer)
                updateTradeOffersAdapter()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }


        // BOB GETS NOTIFIED THAT ALICE CLAIMED THE MONEY AND REVEALED THE SECRET -> HE CLAIMS THE MONEY
        WalletHolder.swapTransactionBroadcastListener.setOnSecretRevealed { secret, offerId ->
            try {
                model.secretRevealed(secret, offerId)
                updateTradeOffersAdapter()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }


        // END OF SWAP
        WalletHolder.swapTransactionConfidenceListener.setOnClaimedConfirmed {
            try {
                model.claimedTransactionConfirmed(it.offerId)
                updateTradeOffersAdapter()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        atomicSwapCommunity.setOnRemove { removeMessage, _ ->
            try {
                model.receivedRemoveMessage(removeMessage)
                updateTradeOffersAdapter()
            } catch (e: Exception) {
                Log.d(LOG, e.stackTraceToString())
            }
        }
    }

    /* call this when user accepts trade offer from Trade Offers screen */
    private fun acceptTrade(tradeOfferItem: TradeOfferItem) {
        try {
            model.acceptTrade(tradeOfferItem.id)
            updateTradeOffersAdapter()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}
