import Foundation

@objc(JailbreakChecker)
public class JailbreakChecker: NSObject {

    private static let jailbreakPaths = [
        // Standard / Rootful Jailbreak Paths
        "/Applications/Cydia.app",
        "/Applications/Sileo.app",
        "/Applications/Zebra.app",
        "/Applications/Blackra1n.app",
        "/usr/sbin/sshd",
        "/usr/libexec/ssh-keysign",
        "/usr/bin/sshd",
        "/etc/apt",
        "/private/var/lib/apt",
        "/private/var/lib/cydia",
        "/private/var/mobile/Library/SBSettings/Themes",
        "/Library/MobileSubstrate/MobileSubstrate.dylib",
        "/var/lib/cydia",

        // Modern Rootless Jailbreak Paths (iOS 15+ / Dopamine / Palera1n)
        "/var/jb",
        "/private/preboot/jb",
        "/var/jb/usr/bin/su",
        "/var/jb/Applications/Sileo.app",
        "/var/jb/usr/libexec/ssh-keysign",
        "/var/jb/etc/apt"
    ]

    @objc public static func isJailbroken() -> Bool {
        return checkPaths() || checkSandboxViolation() || checkFork() || checkSymlinks()
    }

    private static func checkPaths() -> Bool {
        for path in jailbreakPaths {
            if FileManager.default.fileExists(atPath: path) {
                return true
            }
        }
        return false
    }

    private static func checkSandboxViolation() -> Bool {
        let testPath = "/private/jailbreak_sandbox_test.txt"
        do {
            try "sandbox_test".write(toFile: testPath, atomically: true, encoding: .utf8)
            try? FileManager.default.removeItem(atPath: testPath)
            return true // Successfully wrote outside sandbox => Jailbroken
        } catch {
            return false
        }
    }

    private static func checkFork() -> Bool {
        let pointerToFork = UnsafeMutableRawPointer(bitPattern: -2)
        let forkPtr = dlsym(pointerToFork, "fork")
        if forkPtr != nil {
            typealias ForkFunc = @convention(c) () -> Int32
            let fork = unsafeBitCast(forkPtr, to: ForkFunc.self)
            let pid = fork()
            if pid >= 0 {
                if pid > 0 {
                    // Parent process -> kill child
                    var status: Int32 = 0
                    waitpid(pid, &status, 0)
                }
                return true // Fork succeeded => Sandboxing violated
            }
        }
        return false
    }

    private static func checkSymlinks() -> Bool {
        do {
            let attributes = try FileManager.default.attributesOfItem(atPath: "/Applications")
            if let fileType = attributes[.type] as? FileAttributeType, fileType == .typeSymbolicLink {
                return true
            }
        } catch {
        }
        return false
    }
}
