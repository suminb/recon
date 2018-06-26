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
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_main.*
import xyz.brogrammer.apps.android_recon.android.R

class MainActivity : AppCompatActivity() {

    private val PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION = 1001
    private val PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION = 1002

    var cache = HashMap<String, Record>()
    var currentLocation: Location? = null
    lateinit var wifiManager: WifiManager
    lateinit var locationManager: LocationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }

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
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        startScanning()
        startLocationService()
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // Log.d("TEST", "%s".format(wifiManager.scanResults))
            for (result in wifiManager.scanResults) {
                val mac = result.BSSID
                cache[mac] = Record(mac, result.SSID, result.level, result.frequency,
                        result.channelWidth, result.timestamp, result.capabilities)
                // Log.d("TEST", cache[mac].toString())
            }
            // Log.d("TEST", "# of records = %d".format(cache.size))
        }
    }

    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            Log.d("TEST", "%f, %f".format(location.longitude, location.latitude));
            currentLocation = location
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

        Log.d("TEST", "isWifiEnabled? %s".format(wifiManager.isWifiEnabled))
        Log.d("TEST", "isScanAlwaysAvailable? %s".format(wifiManager.isScanAlwaysAvailable))
        Log.d("TEST", "is5GHzBandSupported? %s".format(wifiManager.is5GHzBandSupported))
        Log.d("TEST", "wifiState = %s".format(wifiManager.wifiState))

        wifiManager.startScan()
    }

    private fun stopScanning() {
        unregisterReceiver(broadcastReceiver)
        Log.d("TEST", "stopScanning()")
    }

    private fun startLocationService() {
        try {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 1f, locationListener);
        } catch(ex: SecurityException) {
            Log.e("ERROR", ex.localizedMessage)
        }
    }
}
