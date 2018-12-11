package xyz.brogrammer.apps.recon.android

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.db.*
import xyz.brogrammer.apps.android_recon.android.R
import java.io.File
import java.io.FileWriter

class MainActivity : AppCompatActivity() {

    private val PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION = 1001
    private val PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION = 1002
    private val PERMISSIONS_REQUEST_CODE_WRITE_EXTERNAL_STORAGE = 1003

    var cache = HashMap<String, Record>()
    var currentLocation: Location? = null
    lateinit var wifiManager: WifiManager
    lateinit var locationManager: LocationManager

    var fileWriter: FileWriter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        wifiManager = this.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onStart() {
        super.onStart()

        Log.d("TEST", "SDK_INT = %d".format(Build.VERSION.SDK_INT))
        Log.d("TEST", "Permission check = %d".format(checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION)
        } else {
            startScanning()
        }

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION)
        } else {
            startLocationService()
        }

//        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSIONS_REQUEST_CODE_WRITE_EXTERNAL_STORAGE)
//        }
//        else {
//            fileWriter = FileWriter(File("/sdcard/Download/records.csv"))
//        }

        Log.d("Anko", "Working with database ${database.databaseName}...")
        database.use {
            createTable(
                    "Record", true,
                    "id" to INTEGER + PRIMARY_KEY + AUTOINCREMENT,
                    "mac" to TEXT,
                    "ssid" to TEXT,
                    "signal_strength" to INTEGER,
                    "frequency" to INTEGER,
                    "channel_width" to INTEGER,
                    "timestamp" to INTEGER,
                    "capabilities" to TEXT)
            Log.d("Anko", "Test table has been created")
        }

        database.use {
            insert("Record", "ssid" to "Starbucks")
            insert("Record", "ssid" to "AT&T")
            Log.d("Anko", "Records have been inserted")
        }

        database.use {
            var x = 0
            select("Record", "id", "ssid")
                    .parseList(object: MapRowParser<Record> {
                        override fun parseRow(columns: Map<String, Any?>): Record {
                            val id = columns.getValue("id") as Long
                            val ssid = columns.getValue("ssid") as String
                            val record = Record("(mac)", ssid, 0, 0, 0, 0, "")

                            Log.d("Anko", "Record = $x, $id, $record")
                            x += 1

                            return record
                        }
                    })
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION -> startScanning()
            PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION -> startLocationService()
            PERMISSIONS_REQUEST_CODE_WRITE_EXTERNAL_STORAGE -> fileWriter = FileWriter(File("/sdcard/Download/records.csv"))
        }
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            for (result in wifiManager.scanResults) {
                val mac = result.BSSID
                val record = Record(mac, result.SSID, result.level, result.frequency,
                        result.channelWidth, result.timestamp, result.capabilities)

                cache[mac] = record
                fileWriter?.write(record.asCSV())
            }
            fileWriter?.flush()
            Log.d("FILE", "fileWriter = %s".format(fileWriter))

            val textViewWifiCount = findViewById(R.id.textViewWifiCount) as TextView
            textViewWifiCount.text = "# of Wi-Fi stations found: %d".format(cache.size)
        }
    }

    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            Log.d("TEST", "%f, %f".format(location.longitude, location.latitude));
            currentLocation = location

            val textViewLocation = findViewById(R.id.textViewLocation) as TextView
            textViewLocation.text = "Current location: %f, %f".format(location.latitude, location.longitude)
        }
        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
            Log.d("TEST", "onStatusChanged")
        }
        override fun onProviderEnabled(provider: String) {
            Log.d("TEST", "onProviderEnabled")
        }
        override fun onProviderDisabled(provider: String) {
            Log.d("TEST", "onProviderDisabled")
        }
    }

    private fun startScanning() {
        registerReceiver(broadcastReceiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))

        wifiManager.startScan()
    }

    private fun stopScanning() {
        unregisterReceiver(broadcastReceiver)
    }

    private fun startLocationService() {
        try {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 1f, locationListener);
        } catch(ex: SecurityException) {
            Log.e("ERROR", ex.localizedMessage)
        }
    }
}
