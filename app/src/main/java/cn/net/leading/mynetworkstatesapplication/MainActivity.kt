package cn.net.leading.mynetworkstatesapplication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    val TAG = MainActivity::class.java.simpleName

    /**
     * ConnectivityManager
     */
    private lateinit var mConnectivityManager: ConnectivityManager

    /**
     * 在 5.0 以下版本使用的 网络状态接收器
     */
    private var mNetWorkReceiver: NetworkStatusReceiver? = null

    /**
     * 在 5.0 及以上版本使用的 NetworkCallback
     */
    private var mNetworkCallback: ConnectivityManager.NetworkCallback? = null

    /**
     * 是否需要注销网络状态接收器
     */
    private var needUnregisterReceiver = false

    /**
     * wifi 是否已连接
     */
    private var wifiConnected = false

    /**
     * 移动网络是否已连接
     */
    private var mobileConnected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mConnectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        // 监听网络
        listeningNetwork()
    }

    private fun listeningNetwork() {
        // Android 5.0 及以上，通过
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tv.text = "网络不可用—NetworkCallback"
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            mNetworkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    val activeInfo = mConnectivityManager.activeNetworkInfo
                    val type = activeInfo.type
                    wifiConnected = type == ConnectivityManager.TYPE_WIFI
                    mobileConnected = type == ConnectivityManager.TYPE_MOBILE
                    runOnUiThread {
                        if (wifiConnected) {
                            // wifi已连接
                            tv.text = "wifi已连接—NetworkCallback"
                        } else if (mobileConnected) {
                            // 移动网络已连接
                            tv.text = "移动网络已连接—NetworkCallback"
                        }
                    }
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    Log.e(TAG, "onCapabilitiesChanged,NetworkCapabilities -> $networkCapabilities")
                }

                override fun onLost(network: Network) {
                    runOnUiThread { tv.text = "网络不可用—NetworkCallback" }
                }
            }
            // 需要 android.permission.CHANGE_NETWORK_STATE
            mConnectivityManager.requestNetwork(request, mNetworkCallback);
        } else {
            // 注册网络状态接收者
            registerNetworkStatusReceiver()
        }
    }

    private fun unregisterNetworkCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (mNetworkCallback != null) {
                Log.d(TAG, "Unregistering network callback")
                mConnectivityManager.unregisterNetworkCallback(mNetworkCallback)
            }
        }
    }

    /**
     * 注册网络状态广播接收者
     */
    private fun registerNetworkStatusReceiver() {
        // 向filter中添加 ConnectivityManager.CONNECTIVITY_ACTION 以监听网络
        val intentFilter = IntentFilter()
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)

        mNetWorkReceiver = NetworkStatusReceiver()
        registerReceiver(mNetWorkReceiver, intentFilter)

        needUnregisterReceiver = true

        tv.text = "网络不可用—NetworkStatusReceiver"
    }

    /**
     * 网络状态接收器
     */
    inner class NetworkStatusReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ConnectivityManager.CONNECTIVITY_ACTION) {
                val activeInfo = mConnectivityManager.activeNetworkInfo
                if (activeInfo != null && activeInfo.isConnected && activeInfo.isAvailable) {
                    val type = activeInfo.type
                    wifiConnected = type == ConnectivityManager.TYPE_WIFI
                    mobileConnected = type == ConnectivityManager.TYPE_MOBILE
                    if (wifiConnected) {
                        // wifi已连接
                        tv.text = "wifi已连接—NetworkStatusReceiver"
                    } else if (mobileConnected) {
                        // 移动网络已连接
                        tv.text = "移动网络已连接—NetworkStatusReceiver"
                    }
                } else {
                    // 网络不可用
                    tv.text = "网络不可用—NetworkStatusReceiver"
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (needUnregisterReceiver) {
            if (mNetWorkReceiver != null) {
                // 注销网络状态广播接收器
                unregisterReceiver(mNetWorkReceiver)
            }
        } else {
            unregisterNetworkCallback()
        }
    }

}
