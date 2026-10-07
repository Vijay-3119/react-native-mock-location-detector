let configPlugins;
try {
  configPlugins = require('@expo/config-plugins');
} catch (e) {
  // Graceful fallback if @expo/config-plugins is resolved via parent project
}

const pkg = require('./package.json');

/**
 * Expo Config Plugin to inject required Android permissions for `react-native-mock-location-detector`.
 */
const withMockLocationDetectorPermissions = (config) => {
  if (!configPlugins || !configPlugins.withAndroidManifest) {
    return config;
  }
  return configPlugins.withAndroidManifest(config, async (config) => {
    const androidManifest = config.modResults;

    if (!androidManifest.manifest['uses-permission']) {
      androidManifest.manifest['uses-permission'] = [];
    }

    const permissions = [
      'android.permission.ACCESS_FINE_LOCATION',
      'android.permission.ACCESS_COARSE_LOCATION',
    ];

    permissions.forEach((permission) => {
      const exists = androidManifest.manifest['uses-permission'].some(
        (item) => item.$ && item.$['android:name'] === permission
      );
      if (!exists) {
        androidManifest.manifest['uses-permission'].push({
          $: { 'android:name': permission },
        });
      }
    });

    return config;
  });
};

const withMockLocationDetector = (config) => {
  return withMockLocationDetectorPermissions(config);
};

if (configPlugins && configPlugins.createRunOncePlugin) {
  module.exports = configPlugins.createRunOncePlugin(
    withMockLocationDetector,
    pkg.name,
    pkg.version
  );
} else {
  module.exports = withMockLocationDetector;
}
