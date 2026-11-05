package com.fazlaka.netmanager

import android.bluetooth.BluetoothDevice
import java.util.*

class RaziPrinter {
    private var socket: android.bluetooth.BluetoothSocket? = null
    private val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    fun connect(device: BluetoothDevice): Boolean {
        return try {
            socket = device.createRfcommSocketToServiceRecord(uuid)
            socket?.connect()
            true
        } catch (e: Exception) { false }
    }
    fun printCard(card: MikroTikClient518.Card) {
        val out = socket?.outputStream ?: return
        val ESC = 0x1B.toByte()
        fun w(t: String) { out.write(t.toByteArray(charset("UTF-8"))) }
        out.write(byteArrayOf(ESC, 0x40))
        out.write(byteArrayOf(ESC, 0x61, 0x01))
        w("FAZLAKA\n\n")
        out.write(byteArrayOf(0x1D, 0x21, 0x11))
        w("${card.number}\n")
        out.write(byteArrayOf(0x1D, 0x21, 0x00))
        w("\n")
        out.write(byteArrayOf(ESC, 0x61, 0x00))
        val priceStr = "${card.price}ج"
        val quotaStr = "${card.quotaGb}GB"
        w("$priceStr     $quotaStr\n\n\n")
        out.write(byteArrayOf(0x1D, 0x56, 0x00))
        out.flush()
    }
}
