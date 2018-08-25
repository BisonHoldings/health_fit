import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:health_fit/health_fit.dart';

void main() => runApp(MyApp());

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'HealthFit Demo',
      theme: ThemeData(
        primarySwatch: Colors.blue,
      ),
      home: _MyHomePage(
        title: 'HealthFit Demo Home Page',
      ),
    );
  }
}

class _MyHomePage extends StatefulWidget {
  _MyHomePage({Key key, this.title}) : super(key: key);

  final String title;

  @override
  _MyHomePageState createState() => _MyHomePageState();
}

class _MyHomePageState extends State<_MyHomePage> {
  String _platformVersion = 'Unknown';
  int _stepCount = 0;
  double _weight = 0.0;

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {
    String platformVersion;
    // Platform messages may fail, so we use a try/catch PlatformException.
    try {
      platformVersion = await HealthFit.platformVersion;
    } on PlatformException {
      platformVersion = 'Failed to get platform version.';
    }

    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;

    setState(() {
      _platformVersion = platformVersion;
    });
  }

  @override
  void initState() {
    super.initState();
    initPlatformState();
  }

  void _updateStepCountState() {
    // FIXME: replace with plugin methods
    setState((){
      if (_stepCount == 0) {
        _stepCount = 10000;
      } else {
        _stepCount = 0;
      }

      if (_weight == 0.0) {
        _weight = 65.7;
      } else {
        _weight = 0.0;
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Plugin example app'),
      ),
      body: Column(
        children: <Widget>[
          Container(
            padding: const EdgeInsets.all(16.0),
            child: Center(child: Text('Platform version: $_platformVersion')),
          ),
          Container(
            padding: const EdgeInsets.all(16.0),
            child: Center(child: Text('StepCount: $_stepCount')),
          ),
          Container(
            padding: const EdgeInsets.all(16.0),
            child: Center(child: Text('Weight: $_weight')),
          ),
        ],
        mainAxisAlignment: MainAxisAlignment.center,
        mainAxisSize:  MainAxisSize.max,
      ),
      floatingActionButton: FloatingActionButton(
        child: const Icon(Icons.refresh),
        onPressed: () async {
          _updateStepCountState();
        },
      ),
    );
  }
}
