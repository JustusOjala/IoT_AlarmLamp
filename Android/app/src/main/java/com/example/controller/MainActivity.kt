package com.example.controller

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.bluetooth.*
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.view.View
import android.view.View.OnClickListener
import android.widget.Button
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import java.io.OutputStream
import java.util.UUID

class MainActivity : AppCompatActivity() {
    private var lamps: List<BluetoothDevice>? = emptyList()
    private var oStream: OutputStream? = null
    private var brightness: Int = 255

    private val seekBarListener: OnSeekBarChangeListener = (object : OnSeekBarChangeListener{
        override fun onProgressChanged(seek: SeekBar,
                                       progress: Int, fromUser: Boolean) {
            setBrightness(progress)
        }

        override fun onStartTrackingTouch(seek: SeekBar) {

        }

        override fun onStopTrackingTouch(seek: SeekBar) {
            turnOn()
        }
    })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.button).setOnClickListener{turnOff()}
        findViewById<Button>(R.id.button2).setOnClickListener{turnOn()}
        findViewById<SeekBar>(R.id.seekBar).setOnSeekBarChangeListener(seekBarListener)

        val blM: BluetoothManager = getSystemService(BluetoothManager::class.java)
        val blA: BluetoothAdapter? = blM.adapter

        if(checkSelfPermission("android.permission.BLUETOOTH_SCAN") != PERMISSION_GRANTED) return
        if(checkSelfPermission("android.permission.BLUETOOTH_CONNECT") != PERMISSION_GRANTED) return

        val pairedDevices: Set<BluetoothDevice>? = blA?.bondedDevices
        lamps = pairedDevices?.filter{it.name == "ESP_LAMP" || it.address == "A8:03:2A:6A:2A:52"}
        if(lamps == null || lamps?.isEmpty() == true){
            return
        }

        val socket: BluetoothSocket = lamps?.first()!!.createInsecureRfcommSocketToServiceRecord(lamps!!.first().uuids.first().uuid)
        blA?.cancelDiscovery()
        val a = socket.connectionType
        val b = lamps?.first()?.bondState
        val c = socket.connectionType
        val d = socket.isConnected
        val uuid = lamps!!.first().uuids
        socket.connect()
        oStream = socket.outputStream
    }

    private fun turnOn(){
        val message: ByteArray = ByteArray(2)
        message[0] = 1
        message[1] = brightness.toByte()
        oStream?.write(message) ?: return
    }

    private fun turnOff(){
        oStream?.write(0) ?: return
    }

    private fun setBrightness(b: Int){
        brightness = b
    }
}