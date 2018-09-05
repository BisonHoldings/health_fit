import Flutter

public class SwiftHealthFitPlugin: NSObject, FlutterPlugin {
    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "health_fit", binaryMessenger: registrar.messenger())
        let instance = SwiftHealthFitPlugin()
        registrar.addMethodCallDelegate(instance, channel: channel)
    }

    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        debugPrint(call.method, call.arguments)
        switch call.method {
        case "getPlatformVersion": getPlatformVersion(result)
        case "hasPermission": hasPermission(call: call, result: result)
        case "requestPermission": requestPermission(call: call, result: result)
        case "disable": disable(result)
        case "getData": getData(call: call, result: result)
        default: result(nil)
        }
    }

    private func getPlatformVersion(_ result: @escaping FlutterResult) {
        result("iOS " + UIDevice.current.systemVersion)
    }

    private func hasPermission(call: FlutterMethodCall, result: @escaping FlutterResult) {
        guard
            let args = call.arguments as? [String: Any],
            let dataTypeString = args["dataType"] as? String,
            let dataType = DataType(type: dataTypeString)
        else {
            result(FlutterError())
            return
        }

        result(HealthFit().hasPermission(type: dataType))
    }

    private func requestPermission(call: FlutterMethodCall, result: @escaping FlutterResult) {
        result(true)
    }

    private func disable(_ result: @escaping FlutterResult) {
        result(nil)
    }

    private func getData(call: FlutterMethodCall, result: @escaping FlutterResult) {
        result(nil)
    }
}
