import 'dart:async';

import 'package:flutter/services.dart';

class HealthFit {
  static const MethodChannel _channel =
      const MethodChannel('health_fit');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  static Future<bool> get hasPermission async {
    final bool hasPermission = await _channel.invokeMethod('hasPermission');
    return hasPermission;
  }

  static Future<bool> get requestPermission async {
    final bool requestPermission = await _channel.invokeMethod('requestPermission');
    return requestPermission;
  }

  static Future<bool> get disable async {
    final bool disabled = await _channel.invokeMethod('disable');
    return disabled;
  }

  // FIXME add serializer and return to clients(dart) to the mapped objects
  static Future<String> get data async {
    final String data = await _channel.invokeMethod('getData');
    return data;
  }

}
