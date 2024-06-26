package com.example.bluetoothapplication.ui.dashboard

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.bluetoothapplication.databinding.FragmentDashboardBinding
import org.json.JSONException
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectRequest
import java.io.ByteArrayInputStream
import com.amazonaws.services.s3.model.*


class DashboardFragment : Fragment(),IBackgroundScan {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var dashboardViewModel: DashboardViewModel
    var CHARACTERISTIC_UUID = "beb5483e-36e1-4688-b7f5-ea07361b26a8"
    var SERVICE_UUID = "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
    var gatt:BluetoothGatt? = null
    private var startBackgroundScan: Intent? = null
    private lateinit var notification: TextView
    var ALERT_NOTIFICATION_ID = "ALERT_NOTIFICATION"

    val gattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            Log.i("BluetoothScan onConnectionStateChange", "onConnectionStateChange invoked")
            Log.i("BluetoothScan onConnectionStateChange Device Status", newState.toString() + "")

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i("BluetoothScan", "Connected to device. Proceeding with service discovery");
                activity?.runOnUiThread {
                    val toast = Toast.makeText(requireContext(), "Successfully connected to device", Toast.LENGTH_LONG)
                    toast.setGravity(Gravity.CENTER, 0, 0)
                    toast.show()

                }
                if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    gatt.requestMtu(512)
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                activity?.runOnUiThread {
                    Toast.makeText(requireContext(), "Successfully dis-connected from device", Toast.LENGTH_LONG).show()
                }
                Log.i("Device info", "Disconnecting bluetooth device...")
                gatt.disconnect()
                gatt.close()
            } else {
                Log.i("BluetoothScan", "Could not find any connection state change")
            }
        }
        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            Log.i("BluetoothScan GattConnection", "onMtuChanged invoked")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("BluetoothScan MTU Request","MTU request Success")
                if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    gatt.discoverServices()
                }
            } else {
                Log.i("BluetoothScan MTU Request","MTU request failed")
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            Log.i(" GattConnection", "onCharacteristicChanged invoked")
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            Log.i("BluetoothScan", "onServicesDiscovered invoked")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(UUID.fromString(SERVICE_UUID))
                Log.i("BluetoothScan", "Successfully discovered services of target device")
                if (service != null) {
                    Log.i("BluetoothScan", "Service is not null.")
                    Log.i("BluetoothScan Service", "Found service with UUID: " + service.getUuid().toString());
                    val characteristics = service.characteristics
                    for (c in characteristics) {
                        Log.i("BluetoothScan Characteristic", c.uuid.toString())
                    }
                    val discoveredCharacteristic = service.getCharacteristic(UUID.fromString(CHARACTERISTIC_UUID))
                    if (discoveredCharacteristic != null) {
                        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                            gatt.readCharacteristic(discoveredCharacteristic)
                        }
                        if (gatt.setCharacteristicNotification(discoveredCharacteristic, true)) {
                            Log.i("Set characteristic notification", "Success!")
                            Log.i("Characteristic property flags", discoveredCharacteristic.properties.toString())
                        } else {
                            Log.i("Set characteristic notification", "Failure!")
                        }
                    } else {
                        Log.i("Characteristic info", "Characteristic not found!")
                    }
                } else {
                    Log.i("Service info", "Service not found!")
                }
            } else {
                Log.i("Service Discovery", "Service discovery failed")
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, discoveredCharacteristic: BluetoothGattCharacteristic, value : ByteArray, status: Int) {
            Log.i("GattConnection", "onCharacteristicRead with byte array invoked")
            val value = String(value)
            Log.i("Read data", "Received data: $value")

            if (status == BluetoothGatt.GATT_SUCCESS) {
                val data = discoveredCharacteristic.value
                val value = String(data, StandardCharsets.UTF_8)
                Log.i("Read data", "Received data: $value")
            }
            try {
                Log.i("Smart data","this is smart data")
                val json = JSONObject(value)
                val lastDetected = json.getInt("lastDetected")
                val currentDateTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                val notificationText = "Alert: No motion detected for $lastDetected on $currentDateTime"
                Log.i("last detected",lastDetected.toString())
                activity?.runOnUiThread {
                    Toast.makeText(requireContext(), "Alert: No motion detected for $lastDetected minutes" , Toast.LENGTH_LONG).show()
                    dashboardViewModel.updateNotification(notificationText)
                }
                if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
                ) {
                    Log.i("BluetoothScan","Disconnecting Gatt")
                    gatt.disconnect()
                    gatt.close()
                }

            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            Log.i("GattConnection", "onCharacteristicWrite invoked")
            Log.i("GattConnection", "onCharacteristicWrite invoked: " + characteristic.uuid)
            val data = characteristic.value
            val info = if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            ) {
                gatt.readCharacteristic(characteristic)
            } else {

            }
            info.toString()
            val value = String(data)
            Log.i("Read data", "Received data: $value")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val data = characteristic.value
                val value = String(data, StandardCharsets.UTF_8)
                Log.i("Readdata", "Received data: $value")

            }
        }
    }



    // Initialize Amazon S3 client with region

    private var serviceInstance: SmartWellnessBackgroundScan? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
       dashboardViewModel = ViewModelProvider(this).get(DashboardViewModel::class.java)


        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        notification = binding.notificationText

        dashboardViewModel.notification.observe(viewLifecycleOwner) { notificationText ->
            notification.text = notificationText
        }

        val startscan: Button = binding.startscan
        val stopscan: Button = binding.stopscan

        val onetimescan : Button = binding.onetimescan
        serviceInstance?.callback = this

        onetimescan.setOnClickListener {
            try{
                Log.i("BluetoothScan", "Launch Background Scan(onetimescan) in Wellness Clicked")

                startBackgroundScan = Intent(requireContext(), SmartWellnessBackgroundScan::class.java)
                startBackgroundScan!!.putExtra("service_uuid", SERVICE_UUID)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    requireContext().startForegroundService(startBackgroundScan)
                } else {
                    requireContext().startService(startBackgroundScan)
                }
                val intent = Intent(requireContext(), SmartWellnessBackgroundScan::class.java)
                requireContext().bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
                binding.startscan.isEnabled = false

                Handler(Looper.getMainLooper()).postDelayed({
                    stopBackgroundScan()
                }, 60000 )
            }finally{
                binding.startscan.isEnabled = true

            }
        }

        startscan.setOnClickListener {
            Log.i("BluetoothScan", "Launch Background Scan in Wellness Clicked")
            binding.onetimescan.isEnabled = false
            startBackgroundScan = Intent(requireContext(), SmartWellnessBackgroundScan::class.java)
            startBackgroundScan!!.putExtra("service_uuid", SERVICE_UUID)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requireContext().startForegroundService(startBackgroundScan)
            }else{
                requireContext().startService(startBackgroundScan)
            }
            val intent = Intent(requireContext(), SmartWellnessBackgroundScan::class.java)
            requireContext().bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
        }

        stopscan.setOnClickListener {999
            try{
                stopBackgroundScan()
            }finally{
                binding.onetimescan.isEnabled = true
            }

        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

    }


    override fun onTargetDeviceFound(device: BluetoothDevice) {

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            gatt = device.connectGatt(requireContext(), true, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } else {
            Log.e("BluetoothScan", "Fragment is attached, but permission is not granted.")
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        val bluetoothScanner = bluetoothAdapter.bluetoothLeScanner
        val intent = Intent(context, SmartWellnessBackgroundScan::class.java)
        context.bindService(intent, mConnection, Context.BIND_AUTO_CREATE)

    }



    private val mConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? SmartWellnessBackgroundScan.MyBinder
            if (binder != null) {
                serviceInstance = binder.getService()
                serviceInstance?.registerClient(this@DashboardFragment)
                Log.i("SERVICE BIND SMART WELLNESS", "Success")
            } else {
                Log.e("SERVICE BIND SMART WELLNESS", "Failed to bind to service: Binder is null")
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.i("SERVICE BIND SMART WELLNESS", "Disconnected")
            serviceInstance = null
        }
    }

    private fun stopBackgroundScan() {
        if (startBackgroundScan == null) {
            return
        }
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), "Successfully Dis connected to device", Toast.LENGTH_LONG).show()
        }
        serviceInstance?.stopServiceScan()

        if (serviceInstance != null) {
            requireActivity().stopService(Intent(requireContext(), SmartWellnessBackgroundScan::class.java))
            requireContext().unbindService(mConnection)
        }
        startBackgroundScan = null
    }


}