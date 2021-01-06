package com.notexample.nmeaserialsender

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.app.PendingIntent
import android.content.*
import android.content.pm.PackageManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.location.*
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.felhr.usbserial.UsbSerialDevice
import com.felhr.usbserial.UsbSerialInterface
import kotlinx.android.synthetic.main.activity_main.*

private const val PERMISSION_REQUEST = 10

//class SpinnerActivity : Activity(), AdapterView.OnItemSelectedListener {
//
//    override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
//        // An item was selected. You can retrieve the selected item using
//        // parent.getItemAtPosition(pos)
//    }
//
//    override fun onNothingSelected(parent: AdapterView<*>) {
//        // Another interface callback
//    }
//}

class MainActivity : AppCompatActivity() {
    var last_time_send : Long = 0
    lateinit var m_usbManager: UsbManager
    var m_device: UsbDevice? = null
    var m_serial: UsbSerialDevice? = null
    var m_connection: UsbDeviceConnection? = null
    var was_start_packege_received : Boolean = false
    private var myClipboard: ClipboardManager? = null
    private var myClip: ClipData? = null
    val ACTION_USB_PERMISSION = "permission"
    var baudrate=9600;
    var accepted=false
    private fun startUsbConnecting(){
        val usbDevices: HashMap<String, UsbDevice>? = m_usbManager.deviceList
        if(!usbDevices?.isEmpty()!!){
            var keep = true
            usbDevices.forEach{ entry->
                m_device=entry.value
                val deviceVendorId: Int? = m_device?.vendorId
                if(true){
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




    private lateinit var locationManager: LocationManager

    private var mOnNmeaMessageListener: OnNmeaMessageListener? = null

    private var mLegacyNmeaListener: GpsStatus.NmeaListener? = null


    companion object {
        private const val PERMISSION_REQUEST_CODE = 1
        var counter  = 0


    }
    fun showalertlocation(){
        val dialog2 = Dialog(this)
        dialog2 .requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog2 .setCancelable(false)
        dialog2 .setContentView(R.layout.alert_info_start)
        val yesBtn = dialog2 .findViewById(R.id.button) as Button
        yesBtn.setOnClickListener {
            dialog2 .dismiss()
            accepted=true
            val list = arrayListOf<String>()

            if (ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                list.add(Manifest.permission.ACCESS_FINE_LOCATION)
//                 array = arrayOfNulls<String>(list.size)
//                list.toArray(array)
//                ActivityCompat.requestPermissions(
//                    this,
//                    array,
//                    PERMISSION_REQUEST_CODE
//                )
            }else if (ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                list.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
//                ActivityCompat.requestPermissions(
//                    this,
//                    array,
//                    PERMISSION_REQUEST_CODE
//                )
            }

            val array = arrayOfNulls<String>(list.size)
            list.toArray(array)
            if (list.isEmpty()) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 0.0f, locationListener)
                addNmeaListener()   //if you have all permissions it requests location updates

            } else {
                ActivityCompat.requestPermissions(
                    this,
                    array,

                    PERMISSION_REQUEST_CODE
                )
            }
        }
        dialog2 .show()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val mPrefs = getSharedPreferences("com.notexample.nmeaserialsender", 0)
        baudrate = mPrefs.getInt("baudrate", 9600)

        val spinner: Spinner = findViewById(R.id.baudratespin)
        ArrayAdapter.createFromResource(
            this,
            R.array.baudratearray,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            spinner.adapter = adapter
        }
        var spinnerPosition = getResources().getStringArray(R.array.baudratearray).indexOf(baudrate.toString())
        spinner.setSelection(spinnerPosition)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(p0: AdapterView<*>?) {
                TODO("Not yet implemented")
            }

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                val item:Int = getResources().getStringArray(R.array.baudratearray)[p2].toInt()
                baudrate=item
                val mEditor: SharedPreferences.Editor = mPrefs.edit()
                mEditor.putInt("baudrate", item).apply()


            }

        }

//        when {
//            ContextCompat.checkSelfPermission(
//                this,
//                Manifest.permission.REQUESTED_PERMISSION
//            ) == PackageManager.PERMISSION_GRANTED -> {
//                // You can use the API that requires the permission.
//            }
//            shouldShowRequestPermissionRationale(...) -> {
//            // In an educational UI, explain to the user why your app requires this
//            // permission for a specific feature to behave as expected. In this UI,
//            // include a "cancel" or "no thanks" button that allows the user to
//            // continue using your app without granting the permission.
//            showInContextUI(...)
//        }
//            else -> {
//                // You can directly ask for the permission.
//                // The registered ActivityResultCallback gets the result of this request.
//                requestPermissionLauncher.launch(
//                    Manifest.permission.REQUESTED_PERMISSION)
//            }
//        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED){
            showalertlocation()
        }else if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            showalertlocation()
        }else if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            showalertlocation()
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE) != PackageManager.PERMISSION_GRANTED){
            showalertlocation()
        }
        init()
        m_usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        val filter = IntentFilter()
        filter.addAction(ACTION_USB_PERMISSION)
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        registerReceiver(broadcastReceiver,filter)
        disconnect.setOnClickListener{disconnect()}
        connect.setOnClickListener{startUsbConnecting()}
        try {
            startUsbConnecting()
        }catch (e: Exception){

        }
        try{
        ForegroundService.startService(this, "Serial GPS is running")
        button4.setOnClickListener(View.OnClickListener {
            ForegroundService.stopService(this)
            Toast.makeText(this, "Background service was killed", Toast.LENGTH_SHORT).show()

        })}catch (e: Exception){
            Toast.makeText(this, "Location Permission required", Toast.LENGTH_SHORT).show()
        }
        button5.setOnClickListener(View.OnClickListener {
            try {
                ForegroundService.startService(this,"")
            }catch (e :Exception){

            }
        })
        myClipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager?;
        lat.setOnLongClickListener(View.OnLongClickListener {
            copyText()
            return@OnLongClickListener true
        })


    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar, menu)
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_add -> {
            val dialog = Dialog(this)
            dialog .requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog .setCancelable(false)
            dialog .setContentView(R.layout.alert_info)
            val yesBtn = dialog .findViewById(R.id.button) as Button
            yesBtn.setOnClickListener {
                dialog .dismiss()
            }
            dialog .show()

            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    fun copyText() {
        myClip = ClipData.newPlainText("text", lat.text)
        myClipboard?.setPrimaryClip(myClip ?: return)

        Toast.makeText(this, "Logs Copied", Toast.LENGTH_SHORT).show();
    }
    private fun init() {
        val msg = "Location data not received yet, you may want to check if your location is on"
        lat.text = msg
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }
    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.size != permissions.size) {
            Toast.makeText(this, "Please grant all permission", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 0.0f, locationListener)
            addNmeaListener()
        }

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
    override fun onResume() {
        super.onResume()
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
            addNmeaListener()   //if you have all permissions it requests location updates

        } else if (accepted){
            ActivityCompat.requestPermissions(
                this,
                array,
                PERMISSION_REQUEST_CODE
            )
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
                if(lat.text=="Location data not received yet, you may want to check if your location is on") {
                    lat.text=message
                }else{
                    lat.append(message)
                }

                sendDate(message)     //when your device gets location update it sends it


            }
        }
        locationManager.addNmeaListener(mOnNmeaMessageListener)
    }


    @SuppressLint("MissingPermission")
    override fun onPause() {
        super.onPause()

        locationManager.removeUpdates(locationListener)

        removeNmeaListener()
    }

    private fun removeNmeaListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (mOnNmeaMessageListener != null) {
                locationManager.removeNmeaListener(mOnNmeaMessageListener)
            }
        }
    }
}