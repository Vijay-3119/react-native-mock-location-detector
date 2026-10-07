module.exports = {
  dependency: {
    platforms: {
      android: {
        sourceDir: './android',
        packageImportPath: 'import com.mocklocationdetector.MockLocationDetectorPackage;',
        packageInstance: 'new MockLocationDetectorPackage()',
      },
      ios: {},
    },
  },
};
