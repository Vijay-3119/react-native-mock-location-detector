import Foundation
import DeviceCheck

@objc(MockLocationDetectorSwift)
public class MockLocationDetectorSwift: NSObject {

    private static weak var eventEmitter: RCTEventEmitter?
    private static var timer: Timer?
    private static var isMonitoring = false
    private static var monitoringIntervalMs: Double = 10000.0
    private static var confidenceThreshold: Int = 60

    @objc public static func setEventEmitter(_ emitter: RCTEventEmitter?) {
        self.eventEmitter = emitter
    }

    @objc public static func isMockLocation(
        resolver resolve: @escaping RCTPromiseResolveBlock,
        rejecter reject: @escaping RCTPromiseRejectBlock
    ) {
        let result = evaluateDetection(options: nil)
        if let isMock = result["isMockLocation"] as? Bool {
            resolve(isMock)
        } else {
            resolve(false)
        }
    }

    @objc public static func getDetectionResult(
        options: [String: Any]?,
        resolver resolve: @escaping RCTPromiseResolveBlock,
        rejecter reject: @escaping RCTPromiseRejectBlock
    ) {
        let result = evaluateDetection(options: options)
        resolve(result)
    }

    @objc public static func startMonitoring(
        options: [String: Any]?,
        resolver resolve: @escaping RCTPromiseResolveBlock,
        rejecter reject: @escaping RCTPromiseRejectBlock
    ) {
        if let options = options {
            if let interval = options["intervalMs"] as? Double {
                monitoringIntervalMs = interval
            }
            if let threshold = options["confidenceThreshold"] as? Int {
                confidenceThreshold = threshold
            }
        }

        stopMonitoringInternal()

        isMonitoring = true
        let intervalSeconds = max(1.0, monitoringIntervalMs / 1000.0)

        DispatchQueue.main.async {
            self.timer = Timer.scheduledTimer(withTimeInterval: intervalSeconds, repeats: true) { _ in
                guard isMonitoring else { return }
                let result = evaluateDetection(options: options)
                sendEvent(name: "onDetectionUpdate", body: result)
                if let isMock = result["isMockLocation"] as? Bool, isMock {
                    sendEvent(name: "onMockLocationDetected", body: result)
                }
            }
            resolve(true)
        }
    }

    @objc public static func stopMonitoring(
        resolver resolve: @escaping RCTPromiseResolveBlock,
        rejecter reject: @escaping RCTPromiseRejectBlock
    ) {
        stopMonitoringInternal()
        resolve(true)
    }

    private static func stopMonitoringInternal() {
        isMonitoring = false
        DispatchQueue.main.async {
            timer?.invalidate()
            timer = nil
        }
    }

    private static func evaluateDetection(options: [String: Any]?) -> [String: Any] {
        var confidence = 100
        var reasons: [[String: Any]] = []
        var iosDetails: [String: Any] = [:]

        let threshold = (options?["confidenceThreshold"] as? Int) ?? confidenceThreshold

        // 1. Jailbreak Check
        let isJailbroken = JailbreakChecker.isJailbroken()
        if isJailbroken {
            confidence -= 40
            reasons.append([
                "code": "IOS_JAILBREAK_DETECTED",
                "weight": -40,
                "description": "Jailbreak files, sandbox violations, or rootless jailbreak symlinks detected"
            ])
        }

        // 2. Suspicious Dylib Check
        let dylibs = DylibChecker.getSuspiciousLoadedDylibs()
        if !dylibs.isEmpty {
            confidence -= 30
            reasons.append([
                "code": "IOS_SUSPICIOUS_DYLIB",
                "weight": -30,
                "description": "Known location spoofing or hook dylib loaded: \(dylibs.joined(separator: ", "))"
            ])
        }

        // 3. Debugger Check
        let isDebugger = DebuggerChecker.isDebuggerAttached()
        if isDebugger {
            confidence -= 15
            reasons.append([
                "code": "IOS_DEBUGGER_ATTACHED",
                "weight": -15,
                "description": "Attached debugger detected via sysctl P_TRACED flag"
            ])
        }

        // 4. Simulator Check
        var isSimulator = false
        #if targetEnvironment(simulator)
        isSimulator = true
        confidence -= 50
        reasons.append([
            "code": "IOS_SIMULATOR_DETECTED",
            "weight": -50,
            "description": "Running on iOS Simulator target environment"
        ])
        #endif

        // 5. App Attest Availability Check
        var appAttestSupported = false
        if #available(iOS 14.0, *) {
            appAttestSupported = DCAppAttestService.shared.isSupported
        }

        let finalConfidence = max(0, min(100, confidence))
        let isMockLocation = finalConfidence < threshold

        iosDetails["jailbroken"] = isJailbroken
        iosDetails["simulator"] = isSimulator
        iosDetails["suspiciousDylibsLoaded"] = dylibs
        iosDetails["debuggerAttached"] = isDebugger
        iosDetails["appAttestSupported"] = appAttestSupported

        return [
            "isMockLocation": isMockLocation,
            "confidence": finalConfidence,
            "confidenceThreshold": threshold,
            "reasons": reasons,
            "platform": "ios",
            "timestamp": Date().timeIntervalSince1970 * 1000.0,
            "ios": iosDetails
        ]
    }

    private static func sendEvent(name: String, body: [String: Any]) {
        guard let emitter = eventEmitter else { return }
        emitter.sendEvent(withName: name, body: body)
    }
}
