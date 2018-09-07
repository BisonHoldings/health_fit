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
            let dataType = DataType(rawValue: dataTypeString)
        else {
            result(FlutterError())
            return
        }

        result(HealthFit().hasPermission(type: dataType))
    }

    private func requestPermission(call: FlutterMethodCall, result: @escaping FlutterResult) {
        guard
            let args = call.arguments as? [String: Any],
            let dataTypeString = args["dataType"] as? String,
            let argsPermission = args["permission"] as? Int,
            let dataType = DataType(rawValue: dataTypeString),
            let permission = Permission(rawValue: argsPermission)
            else {
                result(FlutterError())
                return
        }

        HealthFit().requestPermission(
            type: dataType,
            permission: permission,
            completion: { (isSuccess, error) in
                switch (isSuccess, error) {
                case (true, _):
                    result(true)
                case (false, let error?):
                    result(FlutterError(code: "", message: error.localizedDescription, details: nil))
                case (false, nil):
                    result(FlutterError())
                }
        })
    }

    private func disable(_ result: @escaping FlutterResult) {
        result(HealthFit().disable())
    }

    private func getData(call: FlutterMethodCall, result: @escaping FlutterResult) {
        guard
            let args = call.arguments as? [String: Any],
            let dataTypeString = args["dataType"] as? String,
            let dataType = DataType(rawValue: dataTypeString),
            let startAt = args["startAt"] as? Double,
            let endAt = args["endAt"] as? Double,
            let timeUnitString = args["timeUnit"] as? String,
            let timeUnit = TimeUnit(rawValue: timeUnitString)
            else {
                result(FlutterError())
                return
        }

        let startDate = Date(timeIntervalSince1970: startAt/1000)
        let endDate = Date(timeIntervalSince1970: endAt/1000)

        HealthFit().getData(
            startDate: startDate,
            endDate: endDate,
            dataType: dataType,
            timeUnite: timeUnit,
            completion: { result($0)}
        )
    }
}
