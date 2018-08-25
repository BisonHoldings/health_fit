package com.example.healthfit


import android.app.Activity
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.request.DataReadRequest
import java.util.concurrent.TimeUnit
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.PluginRegistry.Registrar
import io.flutter.plugin.common.PluginRegistry.ActivityResultListener


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
    }

    private var deferredResult: Result? = null

    override fun onMethodCall(call: MethodCall, result: Result): Unit {
        when (call.method) {
            "getPlatformVersion" -> {
            }
            "hasPermission" -> hasPermission(call, result)
            "requestPermission" -> requestPermission(call, result)
            "disable" -> disable(result)
            "getData" -> {
                // TODO add getData method with correct arguments
            }
            else -> result.notImplemented()
        }
    }

    private fun hasPermission(call: MethodCall, result: Result) {
        val healthFitOption = getHealthFitOptions(call)
        val optionsBuilder = FitnessOptions.builder()
        optionsBuilder.addDataType(healthFitOption.dataType, healthFitOption.permission)
        result.success(GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(activity), optionsBuilder.build()))
    }

    private fun getHealthFitOptions(call: MethodCall): HealthFitOption =
            HealthFitOption(getDataType(call), getPermission(call))


    private fun getDataType(call: MethodCall): DataType = when (call.argument<String>("dataType")) {
        "com.google.step_count.delta" -> DataType.TYPE_STEP_COUNT_DELTA
        else -> throw IllegalArgumentException("${call.argument<String>("dataType")} is not allowed here.")
    }

    private fun getPermission(call: MethodCall): Int = when (call.argument<Int>("permission")) {
        0 -> FitnessOptions.ACCESS_READ
        1 -> FitnessOptions.ACCESS_WRITE
        else -> throw IllegalArgumentException("${call.argument<Int>("permission")} is not allowed here.")
    }

    private fun disable(result: Result) {
        Fitness.getConfigClient(activity,
                GoogleSignIn.getLastSignedInAccount(activity))
                .disableFit().addOnCompleteListener({
                    result.success(it.isSuccessful)
                })
    }

    private fun requestPermission(call: MethodCall, result: Result) {
        deferredResult = result
        val healthFitOption = getHealthFitOptions(call)
        val optionsBuilder = FitnessOptions.builder()
        optionsBuilder.addDataType(healthFitOption.dataType, healthFitOption.permission)
        GoogleSignIn.requestPermissions(
                activity,
                GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
                GoogleSignIn.getLastSignedInAccount(activity),
                optionsBuilder.build())
    }

    private fun getData(result: Result, dataType: DataType, startAt: Long, endAt: Long, bucket: TimeUnit) {
        val dataRequest = createReadRequest(dataType, startAt, endAt, bucket)
        Fitness.getHistoryClient(activity,
                GoogleSignIn.getLastSignedInAccount(activity))
                .readData(dataRequest)
                .addOnCompleteListener {
                    // TODO add serializer for sending it to dart interface.
                }
    }

    private fun createReadRequest(dataType: DataType,
                                  startAt: Long,
                                  endAt: Long,
                                  bucket: TimeUnit) = DataReadRequest.Builder()
            .read(dataType)
            // TODO making bucket time more flexible
            .bucketByTime(1, bucket)
            .setTimeRange(startAt, endAt, bucket)
            .build()

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent): Boolean = when (resultCode) {
    // TODO: put the concrete messages because requests and results have multiple status.
        Activity.RESULT_OK -> {
            when (requestCode) {
                GOOGLE_FIT_PERMISSIONS_REQUEST_CODE -> {
                    deferredResult?.success(true)
                    deferredResult = null
                    true
                }
                else -> {
                    deferredResult?.success(false)
                    deferredResult = null
                    false
                }
            }
        }
        else -> {
            deferredResult?.success(false)
            deferredResult = null
            false
        }
    }
}

data class HealthFitOption(val dataType: DataType, val permission: Int)
