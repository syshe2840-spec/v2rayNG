package com.v2ray.ang.handler

import android.content.Context
import com.v2ray.ang.AppConfig
import com.v2ray.ang.util.LogUtil
import com.v2ray.ang.util.Utils
import go.Seq
import libv2ray.CoreCallbackHandler
import libv2ray.CoreController
import libv2ray.Libv2ray
import java.util.concurrent.atomic.AtomicBoolean

object V2RayNativeManager {
    private val initialized = AtomicBoolean(false)
    private val coreLoaded = AtomicBoolean(false)
    private var appContext: Context? = null

    fun loadCore(context: Context?) {
        if (coreLoaded.compareAndSet(false, true)) {
            try {
                appContext = context?.applicationContext
                val aliVpnLib = context?.packageManager
                    ?.getApplicationInfo("app.mvpn", 0)
                    ?.nativeLibraryDir + "/libgojni.so"
                System.load(aliVpnLib)
                LogUtil.i(AppConfig.TAG, "AliVPN core loaded: $aliVpnLib")
            } catch (e: Exception) {
                coreLoaded.set(false)
                LogUtil.e(AppConfig.TAG, "Failed to load core", e)
                throw e
            }
        }
    }

    fun initCoreEnv(context: Context?) {
        loadCore(context)
        if (initialized.compareAndSet(false, true)) {
            try {
                Seq.setContext(context?.applicationContext)
                val assetPath = Utils.userAssetPath(context)
                val deviceId = Utils.getDeviceIdForXUDPBaseKey()
                Libv2ray.initCoreEnv(assetPath, deviceId)
                LogUtil.i(AppConfig.TAG, "V2Ray core initialized successfully")
            } catch (e: Exception) {
                initialized.set(false)
                LogUtil.e(AppConfig.TAG, "Failed to initialize V2Ray core", e)
                throw e
            }
        }
    }

    fun newCoreController(handler: CoreCallbackHandler): CoreController {
        return try {
            Libv2ray.newCoreController(handler)
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to create core controller", e)
            throw e
        }
    }

    fun getLibVersion(): String {
        return try {
            Libv2ray.checkVersionX()
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to check V2Ray version", e)
            "Unknown"
        }
    }

    fun measureOutboundDelay(config: String, testUrl: String): Long {
        return try {
            Libv2ray.measureOutboundDelay(config, testUrl)
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to measure outbound delay", e)
            -1L
        }
    }
}
