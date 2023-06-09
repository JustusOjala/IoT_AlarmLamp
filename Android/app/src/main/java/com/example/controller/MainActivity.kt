package com.example.controller

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.bluetooth.*
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.view.View
import android.view.View.OnClickListener
import android.widget.Button
import android.widget.TextView
import java.io.OutputStream
import java.util.UUID

class MainActivity : AppCompatActivity() {
    private var lamps: List<BluetoothDevice>? = emptyList()
    private var oStream: OutputStream? = null
    private var brightness: Int = 255
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.button).setOnClickListener{turnOff()}
        findViewById<Button>(R.id.button2).setOnClickListener{turnOn()}
        //findViewById<Button>(R.id.seekBar).setOnClickListener{turnOn()}

        val blM: BluetoothManager = getSystemService(BluetoothManager::class.java)
        val blA: BluetoothAdapter? = blM.adapter

        if(checkSelfPermission("android.permission.BLUETOOTH_SCAN") != PERMISSION_GRANTED) return
        if(checkSelfPermission("android.permission.BLUETOOTH_CONNECT") != PERMISSION_GRANTED) return

        val pairedDevices: Set<BluetoothDevice>? = blA?.bondedDevices
        lamps = pairedDevices?.filter{it.name == "ESP_LAMP" || it.address == "A8:03:2A:6A:2A:52"}
        if(lamps == null || lamps?.isEmpty() == true){
            return
        }

        val socket: BluetoothSocket = lamps?.first()!!.createRfcommSocketToServiceRecord(UUID.randomUUID())
        oStream = socket.outputStream
    }

    private fun turnOn(){
        val message: ByteArray = ByteArray(2)
        message[0] = 1
        message[1] = -1
        oStream?.write(message) ?: return
    }

    private fun turnOff(){
        oStream?.write(0) ?: return
    }

    private fun setBrightness(){
        brightness = 255
    }
}