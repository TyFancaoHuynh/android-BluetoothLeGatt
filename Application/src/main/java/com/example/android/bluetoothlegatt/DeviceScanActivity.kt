/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.bluetoothlegatt

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.*
import android.widget.*
import java.util.*

class DeviceScanActivity : AppCompatActivity() {

    companion object {
        const val TAG = "DeviceScanActivity"

        const val REQUEST_ENABLE_BT = 0
        const val REQUEST_PERMISSION_LOCATION = 1

        const val PERMISSION_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION

        val DEVICE_ADDRESS_FILTER = LinkedHashSet<String>()

        init {
        }
    }

    private lateinit var layout: View
    private lateinit var mLeDeviceListAdapter: LeDeviceListAdapter
    private var mBluetoothAdapter: BluetoothAdapter? = null

    private var mScanning: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_device_scan)
        layout = findViewById(R.id.main_layout)

        supportActionBar?.setTitle(R.string.title_devices)

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show()
            finish()
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = bluetoothManager.adapter

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).isVisible = false
            menu.findItem(R.id.menu_scan).isVisible = true
            menu.findItem(R.id.menu_refresh).actionView = null
        } else {
            menu.findItem(R.id.menu_stop).isVisible = true
            menu.findItem(R.id.menu_scan).isVisible = false
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_scan -> scanBleDevices(true)
            R.id.menu_stop -> scanBleDevices(false)
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION_LOCATION) {
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                layout.showSnackbar("Location Permission Granted", Snackbar.LENGTH_SHORT)
                scanBleDevices(true)
            } else {
                layout.showSnackbar("Location Permission Denied", Snackbar.LENGTH_SHORT)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish()
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onResume() {
        super.onResume()

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter?.isEnabled!!) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }

        // Initializes list view adapter.
        mLeDeviceListAdapter = LeDeviceListAdapter()
        setListAdapter(mLeDeviceListAdapter)
        scanBleDevices(true)
    }

    /*
    override fun onPause() {
        super.onPause()
        scanBleDevices(false)
        mLeDeviceListAdapter.clear()
    }
    */

    private fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {
        Log.i(TAG, "TODO...")
        val device = mLeDeviceListAdapter.getDevice(position)
        val intent = Intent(this, DeviceControlActivity::class.java)
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.name)
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.address)
        /*
        if (mScanning) {
            scanBleDevices(false)
        }
        */
        startActivity(intent)
    }

    private fun scanBleDevices(enable: Boolean) {
        if (enable) {
            if (checkSelfPermission(PERMISSION_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mLeDeviceListAdapter.clear()
                mScanning = mBluetoothAdapter!!.startLeScan(mLeScanCallback)
            } else {
                if (shouldShowRequestPermissionRationaleCompat(PERMISSION_LOCATION)) {
                    layout.showSnackbar("Location Access Required", Snackbar.LENGTH_INDEFINITE, "OK") {
                        requestPermissionsCompat(arrayOf(PERMISSION_LOCATION),
                                REQUEST_PERMISSION_LOCATION)
                    }

                } else {
                    layout.showSnackbar("Location Permission Not Available", Snackbar.LENGTH_SHORT)
                    requestPermissionsCompat(arrayOf(PERMISSION_LOCATION), REQUEST_PERMISSION_LOCATION)
                }
            }
        } else {
            mScanning = false
            mBluetoothAdapter!!.stopLeScan(mLeScanCallback)
        }
        invalidateOptionsMenu()
    }

    // Device scan callback.
    private val mLeScanCallback = BluetoothAdapter.LeScanCallback { device, _, _ ->
        val macAddress = device.address
        if (DEVICE_ADDRESS_FILTER.size > 0 && !DEVICE_ADDRESS_FILTER.contains(macAddress)) {
            return@LeScanCallback
        }

        runOnUiThread {
            mLeDeviceListAdapter.addDevice(device)
            mLeDeviceListAdapter.notifyDataSetChanged()
        }
    }

    internal class ViewHolder {
        var deviceName: TextView? = null
        var deviceAddress: TextView? = null
    }

    private inner class LeDeviceListAdapter internal constructor() : BaseAdapter() {
        private val mLeDevices: ArrayList<BluetoothDevice> = ArrayList()
        private val mInflator: LayoutInflater = this@DeviceScanActivity.layoutInflater

        fun addDevice(device: BluetoothDevice) {
            if (!mLeDevices.contains(device)) {
                mLeDevices.add(device)
            }
        }

        internal fun getDevice(position: Int): BluetoothDevice {
            return mLeDevices[position]
        }

        internal fun clear() {
            mLeDevices.clear()
        }

        override fun getCount(): Int {
            return mLeDevices.size
        }

        override fun getItem(i: Int): Any {
            return mLeDevices[i]
        }

        override fun getItemId(i: Int): Long {
            return i.toLong()
        }

        override fun getView(i: Int, view: View?, viewGroup: ViewGroup): View? {
            var view = view
            val viewHolder: ViewHolder
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null)
                viewHolder = ViewHolder()
                viewHolder.deviceAddress = view.findViewById(R.id.device_address)
                viewHolder.deviceName = view.findViewById(R.id.device_name)
                view.tag = viewHolder
            } else {
                viewHolder = view.tag as ViewHolder
            }

            val device = mLeDevices[i]
            val deviceName = device.name
            if (deviceName != null && deviceName.isNotEmpty()) {
                viewHolder.deviceName?.text = deviceName
            } else {
                viewHolder.deviceName?.setText(R.string.unknown_device)
            }
            viewHolder.deviceAddress?.text = device.address

            return view
        }
    }

    // BEGIN copied from ListActivity.java
    private var mAdapter: ListAdapter? = null
    private var mList: ListView? = null
    private val mHandler = Handler()
    private var mFinishedStart = false
    private val mRequestFocus = Runnable { mList?.focusableViewAvailable(mList) }

    override fun onRestoreInstanceState(state: Bundle) {
        ensureList()
        super.onRestoreInstanceState(state)
    }

    override fun onDestroy() {
        mHandler.removeCallbacks(mRequestFocus)
        super.onDestroy()
    }

    override fun onContentChanged() {
        super.onContentChanged()
        val emptyView = findViewById<View>(android.R.id.empty)
        mList = findViewById(android.R.id.list)
        if (mList == null) {
            throw RuntimeException(
                    "Your content must have a ListView whose id attribute is " + "'android.R.id.list'")
        }
        if (emptyView != null) {
            mList?.emptyView = emptyView
        }
        mList?.onItemClickListener = mOnClickListener
        if (mFinishedStart) {
            setListAdapter(mAdapter)
        }
        mHandler.post(mRequestFocus)
        mFinishedStart = true
    }

    fun setListAdapter(adapter: ListAdapter?) {
        synchronized(this) {
            ensureList()
            mAdapter = adapter
            mList?.adapter = adapter
        }
    }

    fun setSelection(position: Int) {
        mList?.setSelection(position)
    }

    fun getSelectedItemPosition(): Int {
        return mList!!.selectedItemPosition
    }

    fun getSelectedItemId(): Long {
        return mList!!.selectedItemId
    }

    fun getListView(): ListView? {
        ensureList()
        return mList
    }

    fun getListAdapter(): ListAdapter? {
        return mAdapter
    }

    private fun ensureList() {
        if (mList != null) {
            return
        }
        setContentView(R.layout.list_content_simple)
    }

    private val mOnClickListener = AdapterView.OnItemClickListener { parent, v, position, id -> onListItemClick(parent as ListView, v, position, id) }

    // END copied from ListActivity.java
}
