import Foundation
import MachO

@objc(DylibChecker)
public class DylibChecker: NSObject {

    private static let suspiciousDylibKeywords = [
        "MobileSubstrate",
        "SubstrateLoader",
        "SubstrateInjector",
        "Substitute",
        "ElleKit",
        "Shadow",
        "LocationFaker",
        "Relocate",
        "GPSCheat",
        "FlyJB",
        "LibertyLite",
        "Cycript",
        "frida",
        "SSLKillSwitch",
        "ABBypass"
    ]

    @objc public static func getSuspiciousLoadedDylibs() -> [String] {
        var detected: [String] = []
        let imageCount = _dyld_image_count()

        for i in 0..<imageCount {
            guard let imageNameCStr = _dyld_get_image_name(i) else { continue }
            let imageName = String(cString: imageNameCStr)

            for keyword in suspiciousDylibKeywords {
                if imageName.localizedCaseInsensitiveContains(keyword) {
                    detected.append((imageName as NSString).lastPathComponent)
                    break
                }
            }
        }
        return detected
    }
}
