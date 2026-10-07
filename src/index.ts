import { useEffect, useState } from 'react';
import {
  NativeEventEmitter,
  NativeModules,
  Platform,
} from 'react-native';
import NativeMockLocationDetectorSpec from './NativeMockLocationDetector';
import {
  DetectionEvent,
  DetectionResult,
  MockLocationDetectorError,
  MonitoringOptions,
} from './types';

// TurboModule or legacy NativeModules fallback shim
let MockLocationDetectorNative: any = null;
try {
  MockLocationDetectorNative =
    NativeMockLocationDetectorSpec || NativeModules.MockLocationDetector;
} catch (e) {
  MockLocationDetectorNative = NativeModules.MockLocationDetector || null;
}

if (!MockLocationDetectorNative) {
  console.warn(
    '[MockLocationDetector] Native module `MockLocationDetector` is not installed or linked correctly.'
  );
}

const eventEmitter = MockLocationDetectorNative
  ? new NativeEventEmitter(MockLocationDetectorNative)
  : null;

/**
 * Perform a fast one-shot boolean check to determine if the location is mocked.
 */
export async function isMockLocation(): Promise<boolean> {
  if (!MockLocationDetectorNative) {
    throw new MockLocationDetectorError(
      'NATIVE_MODULE_NOT_FOUND',
      'Native module MockLocationDetector is missing.'
    );
  }
  return await MockLocationDetectorNative.isMockLocation();
}

/**
 * Perform a comprehensive one-shot detection check returning confidence score and full reason breakdown.
 */
export async function getDetectionResult(
  options?: MonitoringOptions
): Promise<DetectionResult> {
  if (!MockLocationDetectorNative) {
    throw new MockLocationDetectorError(
      'NATIVE_MODULE_NOT_FOUND',
      'Native module MockLocationDetector is missing.'
    );
  }
  const rawResult = await MockLocationDetectorNative.getDetectionResult(
    options || {}
  );
  return normalizeResult(rawResult);
}

/**
 * Start periodic native monitoring for location mock updates.
 */
export async function startMonitoring(
  options?: MonitoringOptions
): Promise<boolean> {
  if (!MockLocationDetectorNative) {
    throw new MockLocationDetectorError(
      'NATIVE_MODULE_NOT_FOUND',
      'Native module MockLocationDetector is missing.'
    );
  }
  return await MockLocationDetectorNative.startMonitoring(options || {});
}

/**
 * Stop continuous native monitoring.
 */
export async function stopMonitoring(): Promise<boolean> {
  if (!MockLocationDetectorNative) {
    throw new MockLocationDetectorError(
      'NATIVE_MODULE_NOT_FOUND',
      'Native module MockLocationDetector is missing.'
    );
  }
  return await MockLocationDetectorNative.stopMonitoring();
}

/**
 * Subscribe to continuous monitoring events emitted from the native module.
 */
export function addListener(
  event: DetectionEvent | string,
  listener: (result: DetectionResult) => void
) {
  if (!eventEmitter) {
    throw new MockLocationDetectorError(
      'NATIVE_MODULE_NOT_FOUND',
      'Native EventEmitter is not available.'
    );
  }
  const subscription = eventEmitter.addListener(event, (rawResult: any) => {
    listener(normalizeResult(rawResult));
  });
  return subscription;
}

/**
 * Custom React Hook to continuously monitor mock location detection state.
 *
 * @param options Optional monitoring interval and confidence threshold configurations.
 * @returns DetectionResult state or null while initial check is resolving.
 */
export function useMockLocationDetector(options?: MonitoringOptions): DetectionResult | null {
  const [result, setResult] = useState<DetectionResult | null>(null);

  useEffect(() => {
    let isSubscribed = true;

    // Fetch initial result
    getDetectionResult(options)
      .then((res) => {
        if (isSubscribed) {
          setResult(res);
        }
      })
      .catch((err) => {
        console.warn('[useMockLocationDetector] Initial check error:', err);
      });

    // Subscribe to periodic updates
    let subscription: any = null;
    try {
      subscription = addListener(
        DetectionEvent.DetectionUpdate,
        (newResult: DetectionResult) => {
          if (isSubscribed) {
            setResult(newResult);
          }
        }
      );
    } catch (e) {
      // EventEmitter unavailable
    }

    // Start native monitoring loop
    startMonitoring(options).catch((err) => {
      console.warn('[useMockLocationDetector] Start monitoring error:', err);
    });

    return () => {
      isSubscribed = false;
      if (subscription && typeof subscription.remove === 'function') {
        subscription.remove();
      }
      stopMonitoring().catch(() => {});
    };
  }, [options?.intervalMs, options?.confidenceThreshold, options?.usePlayIntegrity, options?.useAppAttest]);

  return result;
}

/**
 * Normalize native payload into clean DetectionResult TypeScript structure.
 */
function normalizeResult(rawResult: any): DetectionResult {
  const confidence = typeof rawResult?.confidence === 'number' ? rawResult.confidence : 100;
  const confidenceThreshold = typeof rawResult?.confidenceThreshold === 'number' ? rawResult.confidenceThreshold : 60;
  
  return {
    isMockLocation: typeof rawResult?.isMockLocation === 'boolean'
      ? rawResult.isMockLocation
      : confidence < confidenceThreshold,
    confidence: confidence,
    reasons: Array.isArray(rawResult?.reasons) ? rawResult.reasons : [],
    platform: (Platform.OS === 'android' || Platform.OS === 'ios') ? Platform.OS : 'unknown',
    timestamp: typeof rawResult?.timestamp === 'number' ? rawResult.timestamp : Date.now(),
    android: rawResult?.android,
    ios: rawResult?.ios,
  };
}

export * from './types';
