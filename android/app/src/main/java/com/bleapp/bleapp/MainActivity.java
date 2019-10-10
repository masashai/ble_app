package com.bleapp.bleapp;

import android.os.Bundle;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import io.flutter.app.FlutterActivity;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugins.GeneratedPluginRegistrant;

public class MainActivity extends FlutterActivity {
    private static final String CHANNEL = "com.flutter.ble/method1";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GeneratedPluginRegistrant.registerWith(this);

        new MethodChannel(getFlutterView(), CHANNEL).setMethodCallHandler(new MethodChannel.MethodCallHandler() {

            @Override
            public void onMethodCall(MethodCall methodCall, MethodChannel.Result result) {
                final Map<String, Object> args = methodCall.arguments();
                Dictionary<String, String> results = new Hashtable<String, String>();

                results.put("calcResult", "");
                results.put("deviceName", android.os.Build.MODEL);

                // Get Parameters
                int a, b, c;

                try {
                    a = (int) args.get("a");
                    b = (int) args.get("b");
                    c = a * b;
                } catch (Exception ex) {
                    c = -1;
                }

                // Return Results
                if (methodCall.method.equals("Func1")) {
                    if (c == -1) {
                        results.put("calcResult", "Error n/a");
                        result.success(results);
                    } else {
                        results.put("calcResult", "a * b = " + c);
                        result.success(results);
                    }
                }
            }
        });
    }
}