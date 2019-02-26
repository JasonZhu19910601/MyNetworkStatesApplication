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
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    val TAG = MainActivity::class.java.simpleName

    /**
     * 提示网络不可用的 Snackbar
     */
    private var mTipNetworkUnavailableSnackbar: Snackbar? = null

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

    /**
     * 网络不可用
     */
    private fun onNetUnavailable() {
        Log.e(TAG, "onNetUnavailable")
        mTipNetworkUnavailableSnackbar = SnackBarUtil.getInstance().showNoAvailableNetworkSnackBar(this)
    }

    /**
     * 网络可用
     */
    fun onNetAvailable() {
        Log.e(TAG, "onNetAvailable")
        if (mTipNetworkUnavailableSnackbar != null && mTipNetworkUnavailableSnackbar?.isShown!!) {
            mTipNetworkUnavailableSnackbar?.dismiss()
        }
    }

    @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
    private fun listeningNetwork() {
        // Android 5.0 及以上
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // 经过测试，使用 networkCallback 来监测网络状态，
            // 如果app启动前网络可用，那么 onAvailable( ) 方法会被调用;
            // 如果app启动前网络不可用，在app刚启动时，onLost( ) 方法却不会被调用，
            // 所以需要我们自己来监测一下初始网络状态，如果是不可用，则执行无网需要执行的操作
            val activeInfo = mConnectivityManager.activeNetworkInfo
            if (activeInfo == null || !activeInfo.isConnected || !activeInfo.isAvailable) {
                tv.text = "网络不可用—NetworkCallback"
                onNetUnavailable()
            }
            val request = NetworkRequest.Builder()
                // NetworkCapabilities.NET_CAPABILITY_INTERNET 表示此网络应该能够连接到Internet
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                // NetworkCapabilities.TRANSPORT_WIFI 表示该网络使用Wi-Fi传输
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                // NetworkCapabilities.TRANSPORT_CELLULAR 表示此网络使用蜂窝传输
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build()

            mNetworkCallback = object : ConnectivityManager.NetworkCallback() {

                /**
                 * 当 framework 连接并已声明新网络可供使用时调用
                 */
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

                    onNetAvailable()
                }

                /**
                 * 当此请求的 framework 连接到的网络更改功能，但仍满足所述需求时调用。
                 */
                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    Log.e(TAG, "onCapabilitiesChanged,NetworkCapabilities -> $networkCapabilities")
                }

                /**
                 * 丢失网络时调用
                 */
                override fun onLost(network: Network) {
                    runOnUiThread {
                        tv.text = "网络不可用—NetworkCallback"
                        onNetUnavailable()
                    }
                }
            }
            mConnectivityManager.registerNetworkCallback(request, mNetworkCallback)
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
     * 注册网络状态广播接收器
     */
    private fun registerNetworkStatusReceiver() {
        // 向filter中添加 ConnectivityManager.CONNECTIVITY_ACTION 以监听网络
        val intentFilter = IntentFilter()
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)

        mNetWorkReceiver = NetworkStatusReceiver()
        registerReceiver(mNetWorkReceiver, intentFilter)

        needUnregisterReceiver = true
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
                    onNetAvailable()
                } else {
                    // 网络不可用
                    tv.text = "网络不可用—NetworkStatusReceiver"
                    onNetUnavailable()
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
            // 注销 NetworkCallback
            unregisterNetworkCallback()
        }
    }

}
