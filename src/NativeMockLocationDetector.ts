import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export interface Spec extends TurboModule {
  /**
   * One-shot evaluation returning structured detection details.
   */
  getDetectionResult(options?: Object): Promise<Object>;

  /**
   * Fast one-shot boolean evaluation returning if location fix is mocked.
   */
  isMockLocation(): Promise<boolean>;

  /**
   * Start native background timer for periodic mock detection updates.
   */
  startMonitoring(options?: Object): Promise<boolean>;

  /**
   * Stop native background timer.
   */
  stopMonitoring(): Promise<boolean>;

  /**
   * Event emitter listener hooks for RN Codegen.
   */
  addListener(eventName: string): void;
  removeListeners(count: number): void;
}

export default TurboModuleRegistry.getEnforcing<Spec>('MockLocationDetector');
