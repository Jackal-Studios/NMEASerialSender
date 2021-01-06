package com.notexample.nmeaserialsender

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.location.*
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.felhr.usbserial.UsbSerialDevice
import com.felhr.usbserial.UsbSerialInterface
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.Exception

private const val PERMISSION_REQUEST = 10
class ForegroundService : Service()  {
    private val CHANNEL_ID = "ForegroundService Kotlin"
    var last_time_send : Long = 0
    lateinit var m_usbManager: UsbManager
    var m_device: UsbDevice? = null
    var m_serial: UsbSerialDevice? = null
    var m_connection: UsbDeviceConnection? = null
    val ACTION_USB_PERMISSION = "permission"
    var baudrate=9600;

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1
        var counter  = 0

        fun startService(context: Context, message: String) {
            val startIntent = Intent(context, ForegroundService::class.java)
            startIntent.putExtra("inputExtra", message)
            ContextCompat.startForegroundService(context, startIntent)
        }

        fun stopService(context: Context) {
            val stopIntent = Intent(context, ForegroundService::class.java)
            context.stopService(stopIntent)
        }

    }
    private lateinit var locationManager: LocationManager

    private var mOnNmeaMessageListener: OnNmeaMessageListener? = null


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        //do heavy work on a background thread
        val mPrefs = getSharedPreferences("com.notexample.nmeaserialsender", 0)
        baudrate = mPrefs.getInt("baudrate", 9600)
        val input = intent?.getStringExtra("inputExtra")
        createNotificationChannel()
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0, notificationIntent, 0
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("NMEA Serial Sender is running in the background")
            .setContentText(input)
            .setSmallIcon(R.drawable.ic_launcher_foreground1)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)
        //stopSelf();
        try {
            init()
            locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            m_usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
            val filter = IntentFilter()
            filter.addAction(ACTION_USB_PERMISSION)
            filter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED)
            filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            registerReceiver(broadcastReceiver, filter)
            try {
                startUsbConnecting()
            } catch (e: Exception) {

            }

            onRequestPermissionsResult()
            return START_NOT_STICKY
        }catch (e: Exception){
            return START_NOT_STICKY
        }
    }

    private fun init() {
        val msg = "Data not received yet"
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }
    @SuppressLint("MissingPermission")
    fun onRequestPermissionsResult() {
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 0.0f, locationListener)
        addNmeaListener()

    }
    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location?) {


            //Will be implemented later

        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            //
        }

        override fun onProviderEnabled(provider: String?) {
            //
        }

        override fun onProviderDisabled(provider: String?) {
            //
        }

    }
    fun onResume() {
        requestUpdate()
    }

    fun requestUpdate() {
        val list = arrayListOf<String>()

        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            list.add(Manifest.permission.ACCESS_FINE_LOCATION)
        } else if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            list.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }

        val array = arrayOfNulls<String>(list.size)
        list.toArray(array)
        if (list.isEmpty()) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 0.0f, locationListener)
            //setNmeaListener()
            addNmeaListener()
        }
    }
    private fun addNmeaListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            addNmeaListenerAndroidN()
        }
    }


    @SuppressLint("MissingPermission")
    @RequiresApi(api = Build.VERSION_CODES.N)
    private fun addNmeaListenerAndroidN() {
        if (mOnNmeaMessageListener == null) {
            mOnNmeaMessageListener = OnNmeaMessageListener { message, timestamp ->
                //val iuyu = java.util.Calendar.getInstance()
                //val rew = iuyu.timeInMillis
                // if(java.util.Calendar.getInstance().timeInMillis-last_time_send>900){
                //sendDate(message)
                //   last_time_send=java.util.Calendar.getInstance().timeInMillis
                //}

                sendDate(message)
            }
        }
        locationManager.addNmeaListener(mOnNmeaMessageListener)
    }


    private fun startUsbConnecting(){
        val usbDevices: HashMap<String, UsbDevice>? = m_usbManager.deviceList
        if(!usbDevices?.isEmpty()!!){
            var keep = true
            usbDevices.forEach{ entry->
                m_device=entry.value
                val deviceVendorId: Int? = m_device?.vendorId
                if(true){     // should be == "id of your device"
                    val intent: PendingIntent = PendingIntent.getBroadcast(this,0,Intent(ACTION_USB_PERMISSION),0)
                    m_usbManager.requestPermission(m_device,intent)
                    keep = false
                }else{
                    m_connection = null
                    m_device = null


                }
                if (!keep){
                    return
                }
            }
        }
    }
    private fun sendDate(input: String){
        m_serial?.write(input.toByteArray())
    }
    private fun disconnect(){
        m_serial?.close()
    }
    private val broadcastReceiver = object : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            if(intent?.action!! == ACTION_USB_PERMISSION){
                val granted: Boolean = intent.extras!!.getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED)
                if(granted){
                    m_connection = m_usbManager.openDevice(m_device)
                    m_serial = UsbSerialDevice.createUsbSerialDevice(m_device,m_connection)
                    if(m_serial!=null){
                        if(m_serial!!.open()){
                            m_serial!!.setBaudRate(baudrate)
                            m_serial!!.setDataBits(UsbSerialInterface.DATA_BITS_8)
                            m_serial!!.setStopBits(UsbSerialInterface.STOP_BITS_1)
                            m_serial!!.setParity(UsbSerialInterface.PARITY_NONE)
                            m_serial!!.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF)

                        }
                    }
                }
            }else if(intent.action == UsbManager.ACTION_USB_DEVICE_ATTACHED){
                startUsbConnecting()
            }else if(intent.action == UsbManager.ACTION_USB_DEVICE_DETACHED){
                disconnect()
            }
        }

    }
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(CHANNEL_ID, "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT)

            val manager = getSystemService(NotificationManager::class.java)
            manager!!.createNotificationChannel(serviceChannel)
        }
    }

}


