# react-native-mock-gps-detector

[![npm version](https://img.shields.io/npm/v/react-native-mock-gps-detector.svg)](https://www.npmjs.com/package/react-native-mock-gps-detector)
[![license](https://img.shields.io/npm/l/react-native-mock-gps-detector.svg)](https://github.com/Vijay-3119/react-native-mock-location-detector/blob/main/LICENSE)
[![Platform](https://img.shields.io/badge/platform-android%20%7C%20ios-blue.svg)](https://reactnative.dev/)

Production-grade, zero-config-by-default React Native library designed to detect mocked, spoofed, or tampered GPS location signals on Android and iOS. Built with TurboModule Codegen (New Architecture) and full backward compatibility for the Old Architecture.

---

## 🌟 Key Features

- **Weighted Confidence Scoring (0–100):** Avoid fragile binary booleans. Returns a calculated trust score along with specific triggered risk reasons.
- **Android Multi-Signal Detection:**
  - OS Location mock flags (`Location.isMock()` on API 31+, `Location.isFromMockProvider()` on API 18+).
  - AppOps `OPSTR_MOCK_LOCATION` permission check.
  - Package Manager visibility scans for known Fake GPS apps (`<queries>` support).
  - Advanced Root checks (covering `/system/xbin/su`, **KernelSU**, **APatch**, test-keys).
  - Magisk mount point & LSPosed framework stack detection.
  - QEMU & Android Emulator hardware property heuristics.
  - Developer options & ADB debugging state check.
  - Google Play Integrity hardware attestation helper.
- **iOS Multi-Signal Detection:**
  - Rootful & Rootless jailbreak heuristics (`/var/jb`, Dopamine, Palera1n, sandbox violation write tests, process fork tests).
  - Dynamic library injection inspection (`_dyld_get_image_name`) for spoofing tweaks (MobileSubstrate, ElleKit, Shadow, LocationFaker).
  - `sysctl` `P_TRACED` debugger detection.
  - Simulator environment detection.
  - Apple App Attest support check.
- **Expo Compatible:** Zero-config plugin (`app.plugin.js`) for Expo Prebuild / EAS Build.

---

## 🛠️ Installation

```bash
npm install react-native-mock-gps-detector
# or
yarn add react-native-mock-gps-detector
```

### iOS Setup

Run `pod install` inside the `ios` directory:

```bash
cd ios && pod install
```

### Expo Setup

Add the plugin to your `app.json` or `app.config.js`:

```json
{
  "expo": {
    "plugins": ["react-native-mock-gps-detector"]
  }
}
```

---

## 🚀 Quick Usage

### 1. One-Shot Boolean Check

```typescript
import { isMockLocation } from 'react-native-mock-gps-detector';

async function checkLocation() {
  const isMocked = await isMockLocation();
  if (isMocked) {
    console.warn('⚠️ Warning: Location spoofing detected!');
  }
}
```

### 2. Comprehensive Detection Result

```typescript
import { getDetectionResult } from 'react-native-mock-gps-detector';

async function evaluateSecurity() {
  const result = await getDetectionResult({ confidenceThreshold: 60 });

  console.log('Confidence Score:', result.confidence); // 0 (Fake) - 100 (Genuine)
  console.log('Is Mock Location:', result.isMockLocation);

  result.reasons.forEach((reason) => {
    console.log(`- [${reason.code}] Weight: ${reason.weight}: ${reason.description}`);
  });
}
```

### 3. React Hook (Continuous Monitoring)

```typescript
import React from 'react';
import { View, Text } from 'react-native';
import { useMockLocationDetector } from 'react-native-mock-gps-detector';

export function LocationGuardComponent() {
  const result = useMockLocationDetector({ intervalMs: 10000, confidenceThreshold: 60 });

  if (!result) {
    return <Text>Evaluating location trust...</Text>;
  }

  return (
    <View>
      <Text>Trust Score: {result.confidence} / 100</Text>
      <Text>Location Status: {result.isMockLocation ? 'SPOOFED' : 'GENUINE'}</Text>
    </View>
  );
}
```
```

---

## 📖 API Reference

### `isMockLocation(): Promise<boolean>`
Fast one-shot check returning whether confidence score is below threshold (default 60).

### `getDetectionResult(options?: MonitoringOptions): Promise<DetectionResult>`
Returns a full diagnostic breakdown containing:
- `isMockLocation`: boolean
- `confidence`: number (0 to 100)
- `reasons`: Array of `{ code, weight, description }`
- `platform`: `'android'` | `'ios'`
- `android`: Android-specific indicators
- `ios`: iOS-specific indicators

### `startMonitoring(options?: MonitoringOptions): Promise<boolean>`
Starts native background timer polling at specified `intervalMs` (default: 10000ms) emitting `onDetectionUpdate` and `onMockLocationDetected` events.

### `stopMonitoring(): Promise<boolean>`
Stops the native background monitoring timer.

---

## 🛡️ Honest Security Statement & Best Practices

No client-side security signal is unbeatable against a root/kernel-level attacker using sophisticated tools like Magisk Hide, LSPosed, or hardware GPS simulators.

This library is designed to **raise the cost of spoofing** and catch >95% of casual and semi-sophisticated location fakers. For high-security applications (ride-hailing fare calculation, payroll attendance, anti-fraud geofencing), we recommend a multi-layered security posture:

1. **Client Heuristics (This Library):** Rapid, offline, zero-latency detection.
2. **Hardware Attestation:** Google Play Integrity & Apple App Attest tokens verified on your backend server.
3. **Server Behavioral Analysis:** Plausibility checks (speed, teleportation, clock drift audit).

---

## 📄 License

MIT License © 2026 Vijay
