import 'dart:async';

import 'package:flutter/services.dart';

class HealthFit {
  static const MethodChannel _channel = const MethodChannel('health_fit');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  static Future<bool> hasPermission(DataType type) async {
    final bool hasPermission = await _channel.invokeMethod(
        'hasPermission', {"dataType": type.toString(), "permission": 0});
    return hasPermission;
  }

  static Future<bool> requestPermission(DataType type) async {
    final bool requestPermission = await _channel.invokeMethod(
        'requestPermission', {"dataType": type.toString(), "permission": 0});
    return requestPermission;
  }

  static Future<bool> get disable async {
    final bool disabled = await _channel.invokeMethod('disable');
    return disabled;
  }

  // FIXME add serializer and return to clients(dart) to the mapped objects
  static Future<String> getData(DataType type, DateTime startAt, DateTime endAt,
      TimeUnit timeUnit) async {
    final String data = await _channel.invokeMethod('getData', {
      "dataType": type.toString(),
      "startAt": startAt.millisecondsSinceEpoch,
      "endAt": endAt.millisecondsSinceEpoch,
      "timeUnit": timeUnit.toString(),
    });
    return data;
  }
}

enum DataType { STEP, WEIGHT }
enum TimeUnit { MILLISECONDS, HOURS }
