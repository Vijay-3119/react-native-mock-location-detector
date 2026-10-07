/**
 * Structured reason indicating a specific mock/tamper indicator detected on device.
 */
export interface DetectionReason {
  code: string;
  weight: number; // e.g. -60 to 0
  description: string;
}

/**
 * Android-specific detection breakdown.
 */
export interface AndroidDetectionDetails {
  fromMockProvider: boolean;
  mockLocationAppInstalled: boolean;
  mockLocationAppPackage?: string | null;
  rooted: boolean;
  magiskDetected: boolean;
  xposedDetected: boolean;
  emulator: boolean;
  developerOptionsEnabled: boolean;
  playIntegrityVerdict?:
    | 'MEETS_DEVICE_INTEGRITY'
    | 'MEETS_BASIC_INTEGRITY'
    | 'FAILED'
    | 'UNAVAILABLE';
}

/**
 * iOS-specific detection breakdown.
 */
export interface IOSDetectionDetails {
  jailbroken: boolean;
  simulator: boolean;
  suspiciousDylibsLoaded: string[];
  debuggerAttached: boolean;
  appAttestSupported: boolean;
}

/**
 * Consolidated location mock detection result payload.
 */
export interface DetectionResult {
  /**
   * Convenience boolean flag indicating if the location fix is considered spoofed/mocked.
   * Derived from: confidence < confidenceThreshold (default 60).
   */
  isMockLocation: boolean;

  /**
   * Overall location trust confidence score from 0 (guaranteed fake) to 100 (high confidence genuine).
   */
  confidence: number;

  /**
   * Array of individual risk factors / triggers detected by native checkers.
   */
  reasons: DetectionReason[];

  /**
   * Operating system platform ('android' | 'ios').
   */
  platform: 'android' | 'ios' | 'unknown';

  /**
   * Epoch timestamp (ms) when this detection result was generated.
   */
  timestamp: number;

  /**
   * Detailed Android indicators (undefined on iOS).
   */
  android?: AndroidDetectionDetails;

  /**
   * Detailed iOS indicators (undefined on Android).
   */
  ios?: IOSDetectionDetails;
}

/**
 * Options for continuous location mock monitoring.
 */
export interface MonitoringOptions {
  /**
   * Monitoring interval in milliseconds (default: 10000ms).
   */
  intervalMs?: number;

  /**
   * Confidence threshold below which `isMockLocation` flips to true (default: 60).
   */
  confidenceThreshold?: number;

  /**
   * Enable Google Play Integrity token request (Android only). Requires `cloudProjectNumber`.
   */
  usePlayIntegrity?: boolean;

  /**
   * Enable Apple App Attest service evaluation (iOS only, iOS 14+).
   */
  useAppAttest?: boolean;

  /**
   * Google Cloud Project Number required for Play Integrity requests on Android.
   */
  cloudProjectNumber?: string;
}

/**
 * Event names emitted during continuous monitoring.
 */
export enum DetectionEvent {
  DetectionUpdate = 'onDetectionUpdate',
  MockLocationDetected = 'onMockLocationDetected',
  IntegrityCheckFailed = 'onIntegrityCheckFailed',
}

/**
 * Custom error thrown by the native module bridge.
 */
export class MockLocationDetectorError extends Error {
  code:
    | 'UNSUPPORTED_PLATFORM'
    | 'MISSING_PERMISSION'
    | 'INTEGRITY_UNAVAILABLE'
    | 'NATIVE_MODULE_NOT_FOUND';

  constructor(
    code:
      | 'UNSUPPORTED_PLATFORM'
      | 'MISSING_PERMISSION'
      | 'INTEGRITY_UNAVAILABLE'
      | 'NATIVE_MODULE_NOT_FOUND',
    message: string
  ) {
    super(message);
    this.name = 'MockLocationDetectorError';
    this.code = code;
    Object.setPrototypeOf(this, MockLocationDetectorError.prototype);
  }
}
