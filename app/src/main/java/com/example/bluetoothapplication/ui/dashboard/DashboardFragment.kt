package com.example.bluetoothapplication.ui.dashboard

import android.Manifest
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
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
import java.nio.charset.StandardCharsets
import java.util.UUID

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.onConnectionStateChange
    private val binding get() = _binding!!
    var CHARACTERISTIC_UUID = "beb5483e-36e1-4688-b7f5-ea07361b26a8"
    var SERVICE_UUID = "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
    val gattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            Log.i("BluetoothScan", "onConnectionStateChange invoked")
            Log.i("BluetoothScan Device Status", newState.toString() + "")

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i("BluetoothScan", "Connected to device. Proceeding with service discovery");
                activity?.runOnUiThread {
                    Toast.makeText(requireContext(), "Successfully connected to device", Toast.LENGTH_LONG).show()
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

        override fun onCharacteristicRead(gatt: BluetoothGatt, discoveredCharacteristic: BluetoothGattCharacteristic, status: Int) {
            Log.i("GattConnection", "onCharacteristicRead invoked")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    gatt.readCharacteristic(discoveredCharacteristic)
                }

                val characteristicValue = String(discoveredCharacteristic.value, StandardCharsets.UTF_8)
                val data = discoveredCharacteristic.value
                val value = String(data, StandardCharsets.UTF_8)
                Log.i("Read data", "Received data: $value")
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


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val dashboardViewModel =
            ViewModelProvider(this).get(DashboardViewModel::class.java)

        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val notification: TextView = binding.notificationText
        val startscan: Button = binding.startscan
        val stopscan: Button = binding.stopscan

        startscan.setOnClickListener {
            Log.i("BluetoothScan", "Launch Background Scan in Wellness Clicked")
            val startBackgroundScan = Intent(requireContext(), SmartWellnessBackgroundScan::class.java)
            startBackgroundScan.putExtra("service_uuid", SERVICE_UUID)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requireContext().startForegroundService(startBackgroundScan)
            }else{
                requireContext().startService(startBackgroundScan)
            }
        }

        dashboardViewModel.text.observe(viewLifecycleOwner) {
            notification.text = it
        }


        return root
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}