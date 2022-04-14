package nl.tudelft.trustchain.atomicswap

import android.util.Log
import androidx.lifecycle.ViewModel
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.atomicswap.community.TrustChainWrapperAPI
import nl.tudelft.trustchain.atomicswap.messages.AcceptMessage
import nl.tudelft.trustchain.atomicswap.messages.CompleteSwapMessage
import nl.tudelft.trustchain.atomicswap.messages.InitiateMessage
import nl.tudelft.trustchain.atomicswap.messages.TradeMessage
import nl.tudelft.trustchain.atomicswap.swap.Currency
import nl.tudelft.trustchain.atomicswap.swap.Trade
import nl.tudelft.trustchain.atomicswap.swap.WalletAPI
import nl.tudelft.trustchain.atomicswap.swap.WalletHolder
import nl.tudelft.trustchain.atomicswap.ui.enums.TradeOfferStatus
import nl.tudelft.trustchain.atomicswap.ui.swap.LOG
import org.bitcoinj.core.Transaction
import org.bitcoinj.params.RegTestParams
import org.bitcoinj.script.ScriptBuilder

class AtomicSwapViewModel(
    val sender: MessageSender,
    val walletApi: WalletAPI,
    val trustChainWrapperAPI: TrustChainWrapperAPI
) : ViewModel() {

    var trades = mutableListOf<Trade>()
    var tradeOffers = mutableListOf<Pair<Trade, Peer>>()

    fun receivedTradeMessage(trade: TradeMessage, peer: Peer) {
        try {
            Log.i(LOG, "Received new trade offer: " + trade.offerId)
            val newTrade = Trade(
                walletApi,
                trade.offerId.toLong(),
                TradeOfferStatus.OPEN,
                Currency.fromString(trade.toCoin),
                trade.toAmount,
                Currency.fromString(trade.fromCoin),
                trade.fromAmount
            )
            tradeOffers.add(Pair(newTrade, peer))
        } catch (e: Exception) {
            Log.d(LOG, e.stackTraceToString())
        }

    }

    fun receivedAcceptMessage(accept: AcceptMessage, peer: Peer) {

        // find trade in list of my trades
        val trade = trades.first { it.id == accept.offerId.toLong() }
        trade.setOnAccept(accept.btcPubKey.hexToBytes(), accept.ethAddress)
//        Log.d(LOG, "RECEIVED ACCEPT FROM ${peer.mid}")
        Log.d(LOG, "RECEIVED ACCEPT")

        // change status in tradeOffers list
        val tradeOfferItem = tradeOffers.first { it.first.id == trade.id }
        tradeOfferItem.first.status = TradeOfferStatus.IN_PROGRESS

        // create first transaction
        if (walletApi is WalletHolder) {
            if (trade.myCoin == Currency.ETH) {
                createFirstEthereumTransaction(trade, peer)
            } else if (trade.myCoin == Currency.BTC) {
                createFirstBitcoinTransaction(trade, peer, accept.offerId)
            }
        }
    }

    fun createFirstEthereumTransaction(trade: Trade, peer: Peer) {
        val txid = walletApi.ethSwap.createSwap(trade)
        val secretHash = trade.secretHash
        val myPubKey = trade.myPubKey

        if (secretHash == null || myPubKey == null) {
            error("Some fields are not initialised")
        }
        val dataToSend = OnAcceptReturn(
            secretHash = secretHash.toHex(),
            txid,
            myPubKey.toHex(),
            walletApi.getEthAddress()
        )
        sender.sendInitiateMessage(peer, trade.id.toString(), dataToSend)

        Log.d(
            LOG,
            "Alice created an ethereum transaction claimable by bob with id : $txid"
        )
    }

    fun createFirstBitcoinTransaction(trade: Trade, peer: Peer, offerId: String) {
        Log.d(LOG, "generated secret : ${trade.secret?.toHex()}")
        val (transaction, _) = walletApi.bitcoinSwap.createSwapTransaction(trade)

        // add a confidence listener
        walletApi.addInitiatorEntryToConfidenceListener(
            TransactionConfidenceEntry(
                transaction.txId.toString(),
                offerId,
                peer
            )
        )

        // broadcast the transaction
        walletApi.broadcastBitcoinTransaction(transaction)
        // log
        Log.d(
            LOG,
            "Alice created a bitcoin transaction claimable by Bob with id: ${transaction.txId}"
        )
    }

    fun receivedInitiateMessage(initiateMessage: InitiateMessage, peer: Peer) {
        Log.d(LOG, "Bob received initiate message from alice.")

        try {

            // update the trade
            val trade = trades.first { it.id == initiateMessage.offerId.toLong() }
            trade.setOnInitiate(
                initiateMessage.btcPublickey.hexToBytes(),
                initiateMessage.hash.hexToBytes(),
                initiateMessage.txId.hexToBytes(),
                initiateMessage.ethAddress
            )


            if (trade.myCoin == Currency.ETH) {
                Log.d(LOG, "secret hash ${trade.secretHash?.toHex()}")
                val txid = walletApi.ethSwap.createSwap(trade)
                Log.d(
                    LOG,
                    "get swap : ${walletApi.ethSwap.getSwap(trade.secretHash ?: error("")).amount}"
                )
                walletApi.ethSwap.setOnClaimed(
                    trade.secretHash ?: error("we don't know the secret hash")
                ) { secret ->
                    trade.setOnSecretObserved(secret)
                    if (trade.counterpartyCoin == Currency.BTC) {
                        val tx = walletApi.bitcoinSwap.createClaimTransaction(trade)
                        walletApi.broadcastBitcoinTransaction(tx)

                        walletApi.addClaimedEntryToConfidenceListener(
                            TransactionConfidenceEntry(
                                tx.txId.toString(),
                                trade.id.toString(),
                                null
                            )
                        )
                        Log.d(
                            LOG,
                            "Bobs ether was claimed by Alice and the secret was revealed. Bob is now claiming Alice's bitcoin"
                        )
                    }
                }
                sender.sendCompleteMessage(
                    peer,
                    trade.id.toString(),
                    txid
                )

                Log.d(
                    LOG,
                    "Bob receive initiate from ALice and locked ether for alice to claim"
                )

            } else if (trade.myCoin == Currency.BTC) {

                // create a swap transaction
                val (transaction, scriptToWatch) = walletApi.bitcoinSwap.createSwapTransaction(
                    trade
                )

                // add a listener on transaction
                walletApi.addRecipientEntryToConfidenceListener(
                    TransactionConfidenceEntry(
                        transaction.txId.toString(),
                        initiateMessage.offerId,
                        peer
                    )
                )

                // observe Alice spending the transaction
                val watchedAddress =
                    ScriptBuilder.createP2SHOutputScript(scriptToWatch).getToAddress(
                        RegTestParams.get()
                    )
                walletApi.addWatchedAddress(
                    TransactionListenerEntry(
                        watchedAddress,
                        initiateMessage.offerId,
                        scriptToWatch
                    )
                )

                // broadcast transaction
                walletApi.broadcastBitcoinTransaction(transaction)

                // log
                Log.d(LOG, "Bob created a transaction claimable by Alice")
                Log.d(LOG, transaction.toString())
                Log.d(LOG, "Bob's started observing the address $watchedAddress")
            }

        } catch (e: Exception) {
            Log.d(LOG, e.stackTraceToString())
        }
    }

    fun receivedCompleteMessage(completeMessage: CompleteSwapMessage, peer: Peer) {
        val trade = trades.first { it.id == completeMessage.offerId.toLong() }

        if (trade.counterpartyCoin == Currency.ETH) {
            val receipt = walletApi.ethSwap.claimSwap(
                trade.secret ?: error("cannot claim swap, we don't know the secret")
            )
            Log.d(
                LOG,
                "Alice received a complete message from Bob and is now claiming Bob's ether."
            )
            Log.d(LOG, "tx receipt : $receipt")
        } else if (trade.counterpartyCoin == Currency.BTC) {
            val tx = Transaction(RegTestParams(), completeMessage.txId.hexToBytes())
            walletApi.commitBitcoinTransaction(tx)
            trade.setOnComplete(completeMessage.txId.hexToBytes())
            val transaction = walletApi.bitcoinSwap.createClaimTransaction(trade)

            walletApi.addClaimedEntryToConfidenceListener(
                TransactionConfidenceEntry(
                    transaction.txId.toString(),
                    completeMessage.offerId,
                    peer
                )
            )
            walletApi.broadcastBitcoinTransaction(transaction)
            Log.d(LOG, "Alice created a claim transaction")
            Log.d(LOG, transaction.toString())
            val tchain_trans = mapOf(
                AtomicSwapTrustchainConstants.TRANSACTION_FROM_COIN to trade.myCoin.toString(),
                AtomicSwapTrustchainConstants.TRANSACTION_TO_COIN to trade.counterpartyCoin.toString(),
                AtomicSwapTrustchainConstants.TRANSACTION_FROM_AMOUNT to trade.myAmount,
                AtomicSwapTrustchainConstants.TRANSACTION_TO_AMOUNT to trade.counterpartyAmount,
                AtomicSwapTrustchainConstants.TRANSACTION_OFFER_ID to trade.id
            )
            trustChainWrapperAPI.createProposalBlock(
                tchain_trans,
                peer
            )
            Log.d(LOG, "Alice created a trustchain proposal block")
        }

        val tradeOffer = tradeOffers.first { it.first.id == trade.id }
        tradeOffer.first.status = TradeOfferStatus.COMPLETED
        sender.sendRemoveTradeMessage(trade.id.toString())
    }

    fun secretRevealed(secret: ByteArray, offerId: String) {
        try {
            val trade = trades.first { it.id == offerId.toLong() }
            trade.setOnSecretObserved(secret)

            if (trade.counterpartyCoin == Currency.BTC) {
                val transaction = walletApi.bitcoinSwap.createClaimTransaction(trade)

                walletApi.addClaimedEntryToConfidenceListener(
                    TransactionConfidenceEntry(
                        transaction.txId.toString(),
                        offerId,
                        null
                    )
                )
                walletApi.broadcastBitcoinTransaction(transaction)
                Log.d(LOG, "Bob created a claim transaction")
                Log.d(LOG, transaction.toString())
            } else if (trade.counterpartyCoin == Currency.ETH) {
                walletApi.ethSwap.claimSwap(secret)
                Log.d(LOG, "Bob claimed Ethereum. From a secret from bitcoin.")
            }

            val tradeOffer = tradeOffers.first { it.first.id == offerId.toLong() }
            tradeOffer.first.status = TradeOfferStatus.COMPLETED
            sender.sendRemoveTradeMessage(trade.id.toString())

        } catch (e: Exception) {
            Log.d(LOG, e.stackTraceToString())
        }
    }


    // METHODS FOR HANDLING BITCOIN TRANSACTION CONFIRMED EVENTS

    fun transactionInitiatorConfirmed(entry: TransactionConfidenceEntry) {
        val trade = trades.first { it.id == entry.offerId.toLong() }

        val secretHash = trade.secretHash
        val myTransaction = trade.myBitcoinTransaction
        val myPubKey = trade.myPubKey
        val myAddress = trade.myAddress

        if (secretHash == null || myTransaction == null || myPubKey == null || myAddress == null) {
            error("Some fields are not initialised")
        }

        val dataToSend = OnAcceptReturn(
            secretHash.toHex(),
            myTransaction.toHex(),
            myPubKey.toHex(),
            myAddress
        )

        sender.sendInitiateMessage(entry.peer!!, entry.offerId, dataToSend)
        Log.d(LOG, "Alice's transaction is confirmed")
    }

    fun transactionRecipientConfirmed(entry: TransactionConfidenceEntry) {
        val trade = trades.first { it.id == entry.offerId.toLong() }

        val myTransaction =
            trade.myBitcoinTransaction ?: error("Some fields are not initialized")


        // send complete message
        sender.sendCompleteMessage(
            entry.peer!!,
            entry.offerId,
            myTransaction.toHex()
        )
        Log.d(LOG, "Bob's transaction is confirmed")

        val tradeOffer = tradeOffers.first { it.first.id == trade.id }
        tradeOffer.first.status = TradeOfferStatus.COMPLETED
        sender.sendRemoveTradeMessage(trade.id.toString())

    }
}
