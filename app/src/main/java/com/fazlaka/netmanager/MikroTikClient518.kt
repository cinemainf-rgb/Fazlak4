package com.fazlaka.netmanager

import java.io.*
import java.net.Socket

class MikroTikClient518(val host: String, val user: String, val pass: String, val port: Int = 8728) {
    private var socket: Socket? = null
    private var writer: DataOutputStream? = null
    private var reader: DataInputStream? = null

    fun connect(): Boolean {
        return try {
            socket = Socket(host, port)
            writer = DataOutputStream(socket!!.getOutputStream())
            reader = DataInputStream(socket!!.getInputStream())
            login518()
        } catch (e: Exception) { false }
    }

    private fun login518(): Boolean {
        writeSentence(listOf("/login"))
        readSentence()
        writeSentence(listOf("/login", "=name=$user", "=password=$pass"))
        val resp = readSentence()
        return resp.contains("!done")
    }

    data class Profile(val name: String, val quotaGb: Int, val price: Int)
    data class Card(val number: String, val profile: String, val quotaGb: Int, val price: Int, val quotaBytes: Long)

    fun readProfiles(): List<Profile> {
        writeSentence(listOf("/ip/hotspot/user/profile/print"))
        val data = readAll()
        return data.mapNotNull {
            val name = it["name"] ?: return@mapNotNull null
            val quota = Regex("\\d+").find(name)?.value?.toIntOrNull() ?: 0
            if (quota==0) return@mapNotNull null
            val price = when(quota){2->5;5->10;10->20;20->35;50->60 else->35}
            Profile(name, quota, price)
        }
    }

    fun generateCards(profileName: String, customQuotaGb: Int, customPrice: Int, count: Int, digits: Int): List<Card> {
        val cards = mutableListOf<Card>()
        val quotaBytes = customQuotaGb * 1024L * 1024L * 1024L
        for (i in 0 until count) {
            val number = generateNumber(digits)
            writeSentence(listOf("/ip/hotspot/user/add", "=name=$number", "=password=", "=profile=$profileName", "=limit-bytes-total=$quotaBytes", "=comment=Fazlaka-${customQuotaGb}GB-${customPrice}EGP"))
            readSentence()
            cards.add(Card(number, profileName, customQuotaGb, customPrice, quotaBytes))
        }
        return cards
    }

    private fun generateNumber(digits: Int): String {
        val min = Math.pow(10.0, (digits-1).toDouble()).toInt()
        val max = Math.pow(10.0, digits.toDouble()).toInt() - 1
        return (min..max).random().toString()
    }

    fun getActiveUsers(): List<Map<String,String>> {
        writeSentence(listOf("/ip/hotspot/active/print"))
        return readAll()
    }

    fun kickUser(id: String) {
        writeSentence(listOf("/ip/hotspot/active/remove", "=.id=$id"))
        readSentence()
    }

    fun deleteUser(id: String) {
        writeSentence(listOf("/ip/hotspot/user/remove", "=.id=$id"))
        readSentence()
    }

    private fun writeSentence(words: List<String>) {
        for (w in words) writeWord(w)
        writeWord("")
    }
    private fun writeWord(word: String) {
        val bytes = word.toByteArray()
        val len = bytes.size
        if (len < 0x80) writer?.writeByte(len)
        else if (len < 0x4000) { writer?.writeByte((len shr 8) or 0x80); writer?.writeByte(len and 0xFF) }
        writer?.write(bytes)
    }
    private fun readSentence(): String {
        val sb = StringBuilder()
        while (true) { val w = readWord() ?: break; if (w.isEmpty()) break; sb.append(w).append("\n") }
        return sb.toString()
    }
    private fun readAll(): List<Map<String,String>> {
        val list = mutableListOf<Map<String,String>>()
        while (true) {
            val map = mutableMapOf<String,String>()
            while (true) {
                val w = readWord() ?: return list
                if (w.isEmpty()) break
                if (w.startsWith("=")) { val p = w.substring(1).split("=", limit=2); if (p.size==2) map[p[0]]=p[1] }
                else map["_type"]=w
            }
            if (map.isNotEmpty()) list.add(map)
            if (map["_type"]=="!done") break
        }
        return list
    }
    private fun readWord(): String? {
        val first = reader?.read() ?: return null
        val len = if (first and 0x80 == 0) first else { val s = reader!!.read(); ((first and 0x7F) shl 8) + s }
        if (len==0) return ""
        val buf = ByteArray(len)
        reader?.readFully(buf)
        return String(buf)
    }
    fun disconnect() { try { socket?.close() } catch(_:Exception){} }
}
