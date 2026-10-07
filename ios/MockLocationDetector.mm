#import "MockLocationDetector.h"
#if __has_include("react_native_mock_location_detector/react_native_mock_location_detector-Swift.h")
#import "react_native_mock_location_detector/react_native_mock_location_detector-Swift.h"
#else
#import "MockLocationDetector-Swift.h"
#endif

@implementation MockLocationDetector {
  bool hasListeners;
}

RCT_EXPORT_MODULE(MockLocationDetector);

+ (BOOL)requiresMainQueueSetup {
  return NO;
}

- (NSArray<NSString *> *)supportedEvents {
  return @[@"onDetectionUpdate", @"onMockLocationDetected", @"onIntegrityCheckFailed"];
}

- (void)startObserving {
  hasListeners = YES;
  [MockLocationDetectorSwift setEventEmitter:self];
}

- (void)stopObserving {
  hasListeners = NO;
  [MockLocationDetectorSwift setEventEmitter:nil];
}

RCT_EXPORT_METHOD(isMockLocation:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
  [MockLocationDetectorSwift isMockLocationWithResolver:resolve rejecter:reject];
}

RCT_EXPORT_METHOD(getDetectionResult:(NSDictionary *)options
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
  [MockLocationDetectorSwift getDetectionResultWithOptions:options resolver:resolve rejecter:reject];
}

RCT_EXPORT_METHOD(startMonitoring:(NSDictionary *)options
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
  [MockLocationDetectorSwift startMonitoringWithOptions:options resolver:resolve rejecter:reject];
}

RCT_EXPORT_METHOD(stopMonitoring:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
  [MockLocationDetectorSwift stopMonitoringWithResolver:resolve rejecter:reject];
}

@end
