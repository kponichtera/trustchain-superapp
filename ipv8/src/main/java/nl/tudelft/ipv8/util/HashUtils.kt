package nl.tudelft.ipv8.util

import java.security.MessageDigest

private const val SHA1 = "SHA-1"
private const val SHA256 = "SHA-256"
private const val SHA3_256 = "SHA3-256"

fun sha1(input: ByteArray): ByteArray {
    return MessageDigest
        .getInstance(SHA1)
        .digest(input)
}

fun sha256(input: ByteArray): ByteArray {
    return MessageDigest
        .getInstance(SHA256)
        .digest(input)
}

fun sha3_256(input: ByteArray): ByteArray {
    return MessageDigest.getInstance(SHA3_256).digest(input)
}

fun toASCII(value: String): ByteArray {
    return value.toByteArray(Charsets.US_ASCII)
}

fun sha256AsInt(input: ByteArray): Int {
    var out = 0
    val hash = sha256(input)
    for (i in 0..hash.size) {
        out = out shl 8
        out = out or hash[i].toInt()
    }
    return out

}
