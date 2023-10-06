package com.example.controller

import android.app.AlarmManager
import android.app.AlarmManager.RTC_WAKEUP
import android.app.Dialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.icu.util.Calendar
import android.os.Bundle
import android.text.format.DateFormat
import android.text.format.DateFormat.is24HourFormat
import android.widget.Button
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import android.widget.TimePicker
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StartTimePickerFragment : DialogFragment(), TimePickerDialog.OnTimeSetListener {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Use the current time as the default values for the picker.
        val c = Calendar.getInstance()
        val hour = c.get(Calendar.HOUR_OF_DAY)
        val minute = c.get(Calendar.MINUTE)

        // Create a new instance of TimePickerDialog and return it.
        return TimePickerDialog(activity, this, hour, minute, DateFormat.is24HourFormat(activity))
    }

    override fun onTimeSet(view: TimePicker, hourOfDay: Int, minute: Int) {
        val c = Calendar.getInstance()
        val cHour = c.get(Calendar.HOUR_OF_DAY)
        val cMinute = c.get(Calendar.MINUTE)

        if(hourOfDay*60 + minute > cHour*60 + cMinute){
            start = c.timeInMillis - c.get(Calendar.MILLISECONDS_IN_DAY) + hourOfDay*3600000 + minute*60000
        }else{
            start = c.timeInMillis - c.get(Calendar.MILLISECONDS_IN_DAY) + (24+hourOfDay)*3600000 + minute*60000
        }

        ma?.findViewById<Button>(R.id.startTime)?.text = "$hourOfDay:$minute"
    }
}

class EndTimePickerFragment : DialogFragment(), TimePickerDialog.OnTimeSetListener {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Use the current time as the default values for the picker.
        val c = Calendar.getInstance()
        val hour = c.get(Calendar.HOUR_OF_DAY)
        val minute = c.get(Calendar.MINUTE)

        // Create a new instance of TimePickerDialog and return it.
        return TimePickerDialog(activity, this, hour, minute, DateFormat.is24HourFormat(activity))
    }

    override fun onTimeSet(view: TimePicker, hourOfDay: Int, minute: Int) {
        val c = Calendar.getInstance()
        val cHour = c.get(Calendar.HOUR_OF_DAY)
        val cMinute = c.get(Calendar.MINUTE)

        if(hourOfDay*60 + minute > cHour*60 + cMinute){
            end = c.timeInMillis - c.get(Calendar.MILLISECONDS_IN_DAY) + hourOfDay*3600000 + minute*60000
        }else{
            end = c.timeInMillis - c.get(Calendar.MILLISECONDS_IN_DAY) + (24+hourOfDay)*3600000 + minute*60000
        }

        ma?.findViewById<Button>(R.id.endTime)?.text = "$hourOfDay:$minute"
    }
}

class AlarmReceiver : BroadcastReceiver(){
    override fun onReceive(context: Context, intent: Intent) {
        ma?.turnAuto()
    }
}

var start: Long = 0
var end: Long = 0
var ma: MainActivity? = null

class MainActivity : AppCompatActivity() {


    private var lamps: List<BluetoothDevice>? = emptyList()
    private var oStream: OutputStream? = null

    private var brightness: Int = 255

    private var alarmDesc: String = "No alarm set"

    private var startTime: Long = 0 //Brightening start time in millisecond Unix time
    private var endTime: Long = 0
    private var startBrightness: Int = 0
    private var endBrightness: Int = 0

    private var alarmManager: AlarmManager? = null

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
        ma = this

        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.button).setOnClickListener{turnOff()}
        findViewById<Button>(R.id.button2).setOnClickListener{turnOn()}
        findViewById<Button>(R.id.setButton).setOnClickListener{setSlope()}

        findViewById<Button>(R.id.startTime).setOnClickListener {
            StartTimePickerFragment().show(supportFragmentManager, "timePicker")
        }
        findViewById<Button>(R.id.endTime).setOnClickListener {
            EndTimePickerFragment().show(supportFragmentManager, "timePicker")
        }
        findViewById<Button>(R.id.startTime).text = "?:??"
        findViewById<Button>(R.id.endTime).text = "?:??"

        findViewById<TextView>(R.id.description).text = alarmDesc

        findViewById<SeekBar>(R.id.seekBar).setOnSeekBarChangeListener(seekBarListener)

        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

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

    public fun turnAuto(){
        if(startTime > endTime) return

        val message: ByteArray = ByteArray(5)
        message[0] = 2

        //First control point; start at T+0 min
        message[1] = startBrightness.toByte()
        message[2] = 0

        //Second control point
        message[3] = endBrightness.toByte()
        message[4] = ((endTime - startTime)/60000).toByte()

        oStream?.write(message) ?: return
    }

    private fun setBrightness(b: Int){
        brightness = b
    }

    private fun setSlope(){
        startTime = start
        endTime = end

        startBrightness = findViewById<TextView>(R.id.startBright).text.toString().toInt()
        endBrightness = findViewById<TextView>(R.id.endBright).text.toString().toInt()
        findViewById<TextView>(R.id.description).text = alarmDesc
        if(startTime > endTime) return

        val startDate = Date(startTime)
        val endDate = Date(endTime)

        alarmDesc = "Alarm set from $startBrightness at $startDate to $endBrightness at $endDate"
        findViewById<TextView>(R.id.description).text = alarmDesc

        val alarmIntent: Intent = Intent(this, AlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(this, 0, alarmIntent, PendingIntent.FLAG_IMMUTABLE)
        alarmManager?.setExact(RTC_WAKEUP, startTime, pi)
    }
}