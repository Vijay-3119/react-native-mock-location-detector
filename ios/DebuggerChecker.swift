import Foundation

@objc(DebuggerChecker)
public class DebuggerChecker: NSObject {

    @objc public static func isDebuggerAttached() -> Bool {
        var name: [Int32] = [CTL_KERN, KERN_PROC, KERN_PROC_PID, getpid()]
        var info = kinfo_proc()
        var infoSize = MemoryLayout<kinfo_proc>.size

        let result = sysctl(&name, 4, &info, &infoSize, nil, 0)
        if result == 0 {
            return (info.kp_proc.p_flag & P_TRACED) != 0
        }
        return false
    }
}
