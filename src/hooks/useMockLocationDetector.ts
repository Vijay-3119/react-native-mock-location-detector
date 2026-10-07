import { useEffect, useState } from 'react';
import { addListener, getDetectionResult, startMonitoring, stopMonitoring } from '../index';
import { DetectionEvent, DetectionResult, MonitoringOptions } from '../types';

/**
 * Custom React Hook to continuously monitor mock location detection state.
 *
 * @param options Optional monitoring interval and confidence threshold configurations.
 * @returns DetectionResult state or null while initial check is resolving.
 */
export function useMockLocationDetector(options?: MonitoringOptions) {
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
    const subscription = addListener(
      DetectionEvent.DetectionUpdate,
      (newResult: DetectionResult) => {
        if (isSubscribed) {
          setResult(newResult);
        }
      }
    );

    // Start native monitoring loop
    startMonitoring(options).catch((err) => {
      console.warn('[useMockLocationDetector] Start monitoring error:', err);
    });

    return () => {
      isSubscribed = false;
      subscription.remove();
      stopMonitoring().catch(() => {});
    };
  }, [options?.intervalMs, options?.confidenceThreshold, options?.usePlayIntegrity, options?.useAppAttest]);

  return result;
}
