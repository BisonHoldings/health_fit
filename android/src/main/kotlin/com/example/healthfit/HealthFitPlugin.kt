package com.example.healthfit


import android.app.Activity
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.request.DataReadRequest
import java.util.concurrent.TimeUnit


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
            "disable" -> disable(result)
            "getData" -> {
                getData(result)
            }
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

    private fun disable(result: Result) {
        Fitness.getConfigClient(activity,
                GoogleSignIn.getLastSignedInAccount(activity))
                .disableFit().addOnCompleteListener({
                    result.success(it.isSuccessful)
                })
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent): Boolean {
        return true
    }
}
