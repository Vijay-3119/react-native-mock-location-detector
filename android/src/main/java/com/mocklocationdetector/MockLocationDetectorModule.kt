package com.mocklocationdetector

import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule

data class DetectionReason(
    val code: String,
    val weight: Int,
    val description: String
)

data class DetectionResult(
    val isMockLocation: Boolean,
    val confidence: Int,
    val confidenceThreshold: Int,
    val reasons: List<DetectionReason>,
    val platform: String = "android",
    val timestamp: Double = System.currentTimeMillis().toDouble(),
    val fromMockProvider: Boolean,
    val mockLocationAppInstalled: Boolean,
    val mockLocationAppPackage: String?,
    val rooted: Boolean,
    val magiskDetected: Boolean,
    val xposedDetected: Boolean,
    val emulator: Boolean,
    val developerOptionsEnabled: Boolean
) {
    fun toWritableMap(): WritableMap {
        val reasonsArray = Arguments.createArray()
        for (r in reasons) {
            val rMap = Arguments.createMap()
            rMap.putString("code", r.code)
            rMap.putInt("weight", r.weight)
            rMap.putString("description", r.description)
            reasonsArray.pushMap(rMap)
        }

        val androidDetails = Arguments.createMap()
        androidDetails.putBoolean("fromMockProvider", fromMockProvider)
        androidDetails.putBoolean("mockLocationAppInstalled", mockLocationAppInstalled)
        if (mockLocationAppPackage != null) {
            androidDetails.putString("mockLocationAppPackage", mockLocationAppPackage)
        } else {
            androidDetails.putNull("mockLocationAppPackage")
        }
        androidDetails.putBoolean("rooted", rooted)
        androidDetails.putBoolean("magiskDetected", magiskDetected)
        androidDetails.putBoolean("xposedDetected", xposedDetected)
        androidDetails.putBoolean("emulator", emulator)
        androidDetails.putBoolean("developerOptionsEnabled", developerOptionsEnabled)

        val resultMap = Arguments.createMap()
        resultMap.putBoolean("isMockLocation", isMockLocation)
        resultMap.putInt("confidence", confidence)
        resultMap.putInt("confidenceThreshold", confidenceThreshold)
        resultMap.putArray("reasons", reasonsArray)
        resultMap.putString("platform", platform)
        resultMap.putDouble("timestamp", timestamp)
        resultMap.putMap("android", androidDetails)

        return resultMap
    }
}

class MockLocationDetectorModule(private val reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    private val handler = Handler(Looper.getMainLooper())
    private var monitoringRunnable: Runnable? = null
    private var isMonitoring = false
    private var monitoringIntervalMs = 10000L
    private var confidenceThreshold = 60

    private val KNOWN_MOCK_PACKAGES = arrayOf(
        "ru.organizm.fakegps",
        "ru.organizm.mocklocations",
        "com.lexa.fakegps",
        "com.incorporateapps.fakegps",
        "com.incorporateapps.fakegps.fre",
        "com.netspot.fakegps",
        "com.lLocation.pro",
        "com.fakegps.mock",
        "com.marlon.fakegps",
        "com.gogo.fakegps",
        "com.ban.fakegps",
        "com.robert.fakegps",
        "com.tnt.fakegps",
        "com.blogspot.newandroidarchitecture.fakegps",
        "com.lsa.fakegps",
        "com.fly.cool.fakegps",
        "com.location.spoof",
        "cl.jp.fakegps",
        "com.celltower.fakegps",
        "org.fakegps",
        "com.fake.gps.location",
        "com.mock.location",
        "com.king.fakegps",
        "com.py.fakegps",
        "com.dva.mocklocations",
        "com.fsl.mocklocations",
        "com.gps.spoof"
    )

    override fun getName(): String = NAME

    @ReactMethod
    fun isMockLocation(promise: Promise) {
        try {
            val result = evaluateDetectionInternal(null)
            promise.resolve(result.isMockLocation)
        } catch (t: Throwable) {
            promise.reject("DETECTION_ERROR", t.message, t)
        }
    }

    @ReactMethod
    fun getDetectionResult(options: ReadableMap?, promise: Promise) {
        try {
            var threshold: Int? = null
            if (options != null && options.hasKey("confidenceThreshold")) {
                threshold = options.getInt("confidenceThreshold")
            }
            val result = evaluateDetectionInternal(threshold)
            promise.resolve(result.toWritableMap())
        } catch (t: Throwable) {
            promise.reject("DETECTION_ERROR", t.message, t)
        }
    }

    @ReactMethod
    fun startMonitoring(options: ReadableMap?, promise: Promise) {
        try {
            if (options != null) {
                if (options.hasKey("intervalMs")) {
                    monitoringIntervalMs = options.getDouble("intervalMs").toLong()
                }
                if (options.hasKey("confidenceThreshold")) {
                    confidenceThreshold = options.getInt("confidenceThreshold")
                }
            }

            if (isMonitoring) {
                stopMonitoringInternal()
            }

            isMonitoring = true
            monitoringRunnable = object : Runnable {
                override fun run() {
                    if (!isMonitoring) return
                    val result = evaluateDetectionInternal(confidenceThreshold)
                    sendEvent("onDetectionUpdate", result.toWritableMap())
                    if (result.isMockLocation) {
                        sendEvent("onMockLocationDetected", result.toWritableMap())
                    }
                    handler.postDelayed(this, monitoringIntervalMs)
                }
            }
            handler.post(monitoringRunnable as Runnable)
            promise.resolve(true)
        } catch (t: Throwable) {
            promise.reject("MONITORING_ERROR", t.message, t)
        }
    }

    @ReactMethod
    fun stopMonitoring(promise: Promise) {
        stopMonitoringInternal()
        promise.resolve(true)
    }

    @ReactMethod
    fun addListener(eventName: String) {
        // Event emitter boilerplate required for RN Codegen TurboModule
    }

    @ReactMethod
    fun removeListeners(count: Int) {
        // Event emitter boilerplate required for RN Codegen TurboModule
    }

    private fun stopMonitoringInternal() {
        isMonitoring = false
        monitoringRunnable?.let { handler.removeCallbacks(it) }
        monitoringRunnable = null
    }

    private fun evaluateDetectionInternal(thresholdOverride: Int?): DetectionResult {
        var confidence = 100
        val reasons = mutableListOf<DetectionReason>()

        val threshold = thresholdOverride ?: confidenceThreshold
        var detectedPackage: String? = null

        // 1. Location Provider Fix Check
        val lastLocation = getLastKnownLocation()
        var fromMockProvider = false
        if (lastLocation != null) {
            fromMockProvider = isLocationMock(lastLocation)
            if (fromMockProvider) {
                confidence -= 60
                reasons.add(DetectionReason("ANDROID_MOCK_PROVIDER_FLAG", -60, "Location fix marked as mock by OS provider"))
            }
        }

        // 2. System-Wide AppOps Mock Location Provider Scanner
        val activeMockAppPackage = findAppOpsMockLocationProviderPackage()
        val appOpsMockPermission = activeMockAppPackage != null
        if (appOpsMockPermission) {
            confidence -= 50
            detectedPackage = activeMockAppPackage
            reasons.add(DetectionReason("ANDROID_APPOPS_MOCK_ALLOWED", -50, "App Ops mock location permission active for app: $activeMockAppPackage"))
        }

        // 3. Known Fake GPS Apps Scanning
        val installedMockAppPackage = findInstalledMockLocationAppPackage()
        val installedMockApp = installedMockAppPackage != null
        if (installedMockApp) {
            confidence -= 30
            if (detectedPackage == null) {
                detectedPackage = installedMockAppPackage
            }
            reasons.add(DetectionReason("ANDROID_MOCK_APP_INSTALLED", -30, "Known GPS spoofing app package installed: $installedMockAppPackage"))
        }

        // 4. Root Detection (including KernelSU & APatch)
        val isRooted = RootChecker.isRooted(reactContext)
        if (isRooted) {
            confidence -= 20
            reasons.add(DetectionReason("ANDROID_ROOT_DETECTED", -20, "Root su binary or KernelSU/APatch artifacts found"))
        }

        // 5. Magisk / LSPosed Detection
        val isMagisk = MagiskChecker.isMagiskPresent(reactContext)
        if (isMagisk) {
            confidence -= 15
            reasons.add(DetectionReason("ANDROID_MAGISK_DETECTED", -15, "Magisk mount points or binaries detected"))
        }
        val isXposed = MagiskChecker.isXposedOrLSPosedPresent(reactContext)
        if (isXposed) {
            confidence -= 20
            reasons.add(DetectionReason("ANDROID_XPOSED_DETECTED", -20, "LSPosed / Xposed framework stack or binaries present"))
        }

        // 6. Emulator Check
        val isEmulator = EmulatorChecker.isEmulator()
        if (isEmulator) {
            confidence -= 30
            reasons.add(DetectionReason("ANDROID_EMULATOR_DETECTED", -30, "QEMU or Android emulator hardware properties detected"))
        }

        // 7. Developer Settings Check
        val devOptionsEnabled = isDeveloperOptionsEnabled()
        if (devOptionsEnabled) {
            confidence -= 10
            reasons.add(DetectionReason("ANDROID_DEV_OPTIONS_ENABLED", -10, "Developer options or ADB debugging enabled"))
        }

        val finalConfidence = confidence.coerceIn(0, 100)
        val isMockLocation = finalConfidence < threshold

        return DetectionResult(
            isMockLocation = isMockLocation,
            confidence = finalConfidence,
            confidenceThreshold = threshold,
            reasons = reasons,
            platform = "android",
            timestamp = System.currentTimeMillis().toDouble(),
            fromMockProvider = fromMockProvider,
            mockLocationAppInstalled = installedMockApp || appOpsMockPermission,
            mockLocationAppPackage = detectedPackage,
            rooted = isRooted,
            magiskDetected = isMagisk,
            xposedDetected = isXposed,
            emulator = isEmulator,
            developerOptionsEnabled = devOptionsEnabled
        )
    }

    private fun isLocationMock(location: Location): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (location.isMock) return true
        }
        @Suppress("DEPRECATION")
        if (location.isFromMockProvider) return true

        if (location.provider == "mock") return true

        val extras = location.extras
        if (extras != null && extras.getBoolean("mockLocation", false)) {
            return true
        }
        return false
    }

    private fun getLastKnownLocation(): Location? {
        val locationManager = reactContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null
        var bestLocation: Location? = null
        try {
            val providers = locationManager.getProviders(false)
            for (provider in providers) {
                val l = locationManager.getLastKnownLocation(provider) ?: continue
                if (bestLocation == null || l.accuracy < bestLocation.accuracy) {
                    bestLocation = l
                }
            }
        } catch (ignored: SecurityException) {
        }
        return bestLocation
    }

    private fun findAppOpsMockLocationProviderPackage(): String? {
        return try {
            val appOps = reactContext.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val pm = reactContext.packageManager

            val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledPackages(0)
            }

            for (pkg in packages) {
                val packageName = pkg.packageName
                if (packageName == reactContext.packageName) continue
                val uid = pkg.applicationInfo?.uid ?: continue

                val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    appOps.unsafeCheckOpNoThrow(
                        AppOpsManager.OPSTR_MOCK_LOCATION,
                        uid,
                        packageName
                    )
                } else {
                    @Suppress("DEPRECATION")
                    appOps.checkOpNoThrow(
                        AppOpsManager.OPSTR_MOCK_LOCATION,
                        uid,
                        packageName
                    )
                }
                if (mode == AppOpsManager.MODE_ALLOWED) {
                    return packageName
                }
            }
            null
        } catch (t: Throwable) {
            null
        }
    }

    private fun findInstalledMockLocationAppPackage(): String? {
        val pm = reactContext.packageManager
        
        // 1. Direct package check against known array
        for (pkg in KNOWN_MOCK_PACKAGES) {
            try {
                pm.getPackageInfo(pkg, 0)
                return pkg
            } catch (e: PackageManager.NameNotFoundException) {
            }
        }

        // 2. Fallback pattern search across all installed packages
        try {
            val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledPackages(0)
            }

            for (pkg in packages) {
                val name = pkg.packageName.lowercase()
                if (name == reactContext.packageName.lowercase()) continue
                if (name.contains("fakegps") || name.contains("mocklocation") ||
                    name.contains("spooflocation") || name.contains("gpsspoof") ||
                    name.contains("mock.location") || name.contains("fake.gps")
                ) {
                    return pkg.packageName
                }
            }
        } catch (ignored: Throwable) {
        }

        return null
    }

    private fun isDeveloperOptionsEnabled(): Boolean {
        return try {
            val devEnabled = Settings.Global.getInt(
                reactContext.contentResolver,
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0
            ) != 0
            val adbEnabled = Settings.Global.getInt(
                reactContext.contentResolver,
                Settings.Global.ADB_ENABLED, 0
            ) != 0
            devEnabled || adbEnabled
        } catch (t: Throwable) {
            false
        }
    }

    private fun sendEvent(eventName: String, params: WritableMap) {
        if (reactContext.hasActiveReactInstance()) {
            reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                .emit(eventName, params)
        }
    }

    companion object {
        const val NAME = "MockLocationDetector"
    }
}
