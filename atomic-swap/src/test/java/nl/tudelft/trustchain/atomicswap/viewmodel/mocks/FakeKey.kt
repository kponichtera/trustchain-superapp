package nl.tudelft.trustchain.atomicswap.viewmodel.mocks

import nl.tudelft.ipv8.keyvault.Key
import nl.tudelft.ipv8.keyvault.PublicKey

class FakeKey : Key {
    override fun pub(): PublicKey {
        TODO("Not yet implemented")
    }

    override fun keyToBin(): ByteArray {
        TODO("Not yet implemented")
    }
}
