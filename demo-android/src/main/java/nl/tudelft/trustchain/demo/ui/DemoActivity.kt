package nl.tudelft.trustchain.demo.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.os.Build
import android.net.Uri
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.android.synthetic.main.fragment_peers.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.trustchain.demo.DemoApplication
import nl.tudelft.trustchain.demo.DemoCommunity
import nl.tudelft.trustchain.demo.R
import nl.tudelft.trustchain.demo.ui.peers.AddressItem
import nl.tudelft.trustchain.demo.ui.peers.AddressItemRenderer
import nl.tudelft.trustchain.demo.ui.peers.PeerItem
import nl.tudelft.trustchain.demo.ui.peers.PeerItemRenderer

@RequiresApi(Build.VERSION_CODES.M)
class DemoActivity : AppCompatActivity() {
    private val adapter = ItemAdapter()

    private val BLUETOOTH_PERMISSIONS_REQUEST_CODE = 200
    private val SETTINGS_INTENT_CODE = 1000

    private val BLUETOOTH_PERMISSIONS_SCAN = "android.permission.BLUETOOTH_SCAN"
    private val BLUETOOTH_PERMISSIONS_CONNECT = "android.permission.BLUETOOTH_CONNECT"
    private val BLUETOOTH_PERMISSIONS_ADVERTISE = "android.permission.BLUETOOTH_ADVERTISE"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle init of IPv8 after requesting permissions; only if Android 12 or higher.
        // onPermissionsDenied() is run until user has accepted permissions.
        val BUILD_VERSION_CODE_S = 31
        if (Build.VERSION.SDK_INT >= BUILD_VERSION_CODE_S) {
            if (!hasBluetoothPermissions()) {
                requestBluetoothPermissions()
            } else {
                // Only initialize IPv8 if it has not been initialized yet.
                try {
                    IPv8Android.getInstance()
                } catch (exception: Exception) {
                    (application as DemoApplication).initIPv8()
                }
            }
        }

        setContentView(R.layout.fragment_peers)

        adapter.registerRenderer(PeerItemRenderer {
            // NOOP
        })

        adapter.registerRenderer(AddressItemRenderer {
            // NOOP
        })

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.addItemDecoration(DividerItemDecoration(this, LinearLayout.VERTICAL))

        loadNetworkInfo()
        //receiveGossips()
    }

    private fun hasBluetoothPermissions(): Boolean {
        return checkSelfPermission(BLUETOOTH_PERMISSIONS_ADVERTISE) == PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(BLUETOOTH_PERMISSIONS_CONNECT) == PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(BLUETOOTH_PERMISSIONS_SCAN) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestBluetoothPermissions() {
        requestPermissions(
            arrayOf(
                BLUETOOTH_PERMISSIONS_ADVERTISE,
                BLUETOOTH_PERMISSIONS_CONNECT,
                BLUETOOTH_PERMISSIONS_SCAN
            ),
            BLUETOOTH_PERMISSIONS_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            BLUETOOTH_PERMISSIONS_REQUEST_CODE -> {
                if (hasBluetoothPermissions()) {
                    (application as DemoApplication).initIPv8()
                } else {
                    onPermissionsDenied()
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            SETTINGS_INTENT_CODE -> {
                if (hasBluetoothPermissions()) {
                    (application as DemoApplication).initIPv8()
                } else {
                    onPermissionsDenied()
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun onPermissionsDenied() {
        AlertDialog.Builder(this)
            .setMessage(getString(R.string.permissions_denied_message))
            .apply {
                setPositiveButton(getString(R.string.permissions_denied_ok_button)) { _, _ ->
                    run {
                        val intent =
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        val uri: Uri = Uri.fromParts("package", packageName, null)
                        intent.data = uri
                        startActivityForResult(intent, SETTINGS_INTENT_CODE)
                    }
                }.create()
            }
            .show()
            .setCanceledOnTouchOutside(false)
    }

    private fun loadNetworkInfo() {
        lifecycleScope.launchWhenStarted {
            while (isActive) {
                val demoCommunity = IPv8Android.getInstance().getOverlay<DemoCommunity>()!!
                val peers = demoCommunity.getPeers()

                val discoveredAddresses = demoCommunity.network
                    .getWalkableAddresses(demoCommunity.serviceId)

                val discoveredBluetoothAddresses = demoCommunity.network
                    .getNewBluetoothPeerCandidates()
                    .map { it.address }

                val peerItems = peers.map {
                    PeerItem(
                        it
                    )
                }

                val addressItems = discoveredAddresses.map { address ->
                    val contacted = demoCommunity.discoveredAddressesContacted[address]
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
                    Log.d("DemoCommunity", "FOUND PEER with id " + peer.mid)
                }


                adapter.updateItems(items)
                txtCommunityName.text = demoCommunity.javaClass.simpleName
                txtPeerCount.text = "${peers.size} peers"
                val textColorResId = if (peers.isNotEmpty()) R.color.green else R.color.red
                val textColor = ResourcesCompat.getColor(resources, textColorResId, null)
                txtPeerCount.setTextColor(textColor)
                imgEmpty.isVisible = items.isEmpty()
                demoCommunity.broadcastTradeOffer(1, 0.5)

                delay(3000)
            }
        }
    }
}
