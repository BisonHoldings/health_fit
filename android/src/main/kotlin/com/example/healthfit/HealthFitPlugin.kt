package com.example.healthfit

import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodCall
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import io.flutter.plugin.common.PluginRegistry.Registrar
import io.flutter.plugin.common.PluginRegistry.ActivityResultListener
import android.app.Activity
import java.text.DateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import android.content.Intent
import android.support.v4.content.PermissionChecker
import io.flutter.plugin.common.MethodChannel.Result


import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.gms.fitness.result.DataReadResponse
import com.google.android.gms.tasks.Tasks
import io.flutter.plugin.common.PluginRegistry


class HealthFitPlugin(private val activity: Activity) : MethodCallHandler, ActivityResultListener {

    companion object {
        const val GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 1
        @JvmStatic
        fun registerWith(registrar: Registrar): Unit {
            val plugin = HealthFitPlugin(registrar.activity())
            registrar.addActivityResultListener(plugin)

            val channel = MethodChannel(registrar.messenger(), "health_fit")
            channel.setMethodCallHandler(plugin);
        }

        val dataType: DataType = DataType.TYPE_STEP_COUNT_DELTA
        val aggregatedDataType: DataType = DataType.AGGREGATE_STEP_COUNT_DELTA

        val TAG = HealthFitPlugin::class.java.simpleName
    }

    override fun onMethodCall(call: MethodCall, result: Result): Unit {
        when (call.method) {
            "getPlatformVersion" -> {
            }
            "hasPermission" -> hasPermission(call, result)
            else -> result.notImplemented()
        }
    }

    private fun hasPermission(call: MethodCall, result: Result) {
        val rawDataType = call.argument("dataType")
        val rawPermission = call.argument("permission")
        val healthFitOptions = mutableSetOf<String>()
        val dataType: DataType = when (rawDataType) {
            "com.google.step_count.delta" -> DataType.TYPE_STEP_COUNT_DELTA
            else -> throw IllegalArgumentException("$rawDataType is not allowed here.")
        }
        val permission: Int = when (rawPermission) {
            0 -> FitnessOptions.ACCESS_READ
            1 -> FitnessOptions.ACCESS_WRITE
            else -> throw IllegalArgumentException("$rawPermission is not allowed here.")
        }
        val optionsBuilder = FitnessOptions.builder()
        optionsBuilder.addDataType(dataType, permission)
        result.success(GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(activity), optionsBuilder.build()))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent): Boolean {
        return true
    }
}
