package com.example.controller

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.bluetooth.*
import android.content.Intent

class MainActivity : AppCompatActivity() {

    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)

    private fun btCB(){

    }
    override fun onCreate(savedInstanceState: Bundle?) {
        val blM: BluetoothManager = getSystemService(BluetoothManager::class.java)
        val blA: BluetoothAdapter? = blM.adapter

        if(blA?.isEnabled == false){

        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}