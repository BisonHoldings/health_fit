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
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.Scope
import com.google.android.gms.fitness.HistoryApi
import java.util.*


class HealthFitPlugin(private val activity: Activity) : MethodCallHandler, ActivityResultListener {

    companion object {
        const val GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 1
        const val GOOGLE_SIGN_IN_REQUEST_CODE = 2
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
        Log.e("HFP: onMethodCall", "${call.method}")
        when (call.method) {
            "getPlatformVersion" -> {
            }
            "hasPermission" -> hasPermission(call, result)
            "requestPermission" -> requestPermission(call, result)
            "disable" -> disable(result)
            "getData" -> {
                val cal = Calendar.getInstance()
                val now = Date()
                cal.setTime(now)
                val endTime = cal.getTimeInMillis()
                cal.add(Calendar.MONTH, -1)
                val startTime = cal.getTimeInMillis()
                getData(result, DataType.TYPE_STEP_COUNT_DELTA, startTime, endTime, TimeUnit.DAYS)
                // TODO add getData method with correct arguments
            }
            else -> result.notImplemented()
        }
    }

    private fun hasPermission(call: MethodCall, result: Result) {
        val healthFitOption = getHealthFitOptions(call)
        val optionsBuilder = FitnessOptions.builder()
        optionsBuilder.addDataType(healthFitOption.dataType, healthFitOption.permission)
        Log.e("HFP: hasPermission option", "${healthFitOption.dataType}:${healthFitOption.permission}")
        Log.e("HFP: hasPermission Account", "${GoogleSignIn.getLastSignedInAccount(activity)}")
        Log.e("HFP: hasPermission", "${GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(activity), optionsBuilder.build())}")
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
        GoogleSignIn.getLastSignedInAccount(activity)?.let {
            Fitness.getConfigClient(activity, it)
                    .disableFit().addOnCompleteListener({
                        result.success(it.isSuccessful)
                    })
        }

        if (GoogleSignIn.getLastSignedInAccount(activity) == null) {
            attemptSignIn {
                if (it) {
                    disable(result)
                } else {
                    Log.e("HFP: disable", "failed to disable")
                }
            }
        }

    }

    private fun attemptSignIn(callback: (Boolean) -> Unit) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build()
        activity.startActivityForResult(GoogleSignIn.getClient(activity, gso).signInIntent, GOOGLE_SIGN_IN_REQUEST_CODE)
//                .addOnSuccessListener {
//                    Log.e("HFP: attemptSignIn", "success")
//                    callback(true)
//                }
//                .addOnFailureListener {
//                    Log.e("HFP: attemptSignIn", "failure")
//                    callback(false)
//                }
    }

    private fun requestPermission(call: MethodCall, result: Result) {
        Log.e("HFP: requestPermission", "result")
        deferredResult = result
        val healthFitOption = getHealthFitOptions(call)
        val optionsBuilder = FitnessOptions.builder()
        optionsBuilder.addDataType(healthFitOption.dataType, healthFitOption.permission)
        GoogleSignIn.getLastSignedInAccount(activity)?.let {
            GoogleSignIn.requestPermissions(
                    activity,
                    GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
                    it,
                    optionsBuilder.build())
        }

        if (GoogleSignIn.getLastSignedInAccount(activity) == null) {
            attemptSignIn {
                if (it) {
                    requestPermission(call, result)
                } else {
                    Log.e("HFP: requestPermission", "failed to request permission")
                }
            }
        }
    }

    private fun getData(result: Result, dataType: DataType, startAt: Long, endAt: Long, bucket: TimeUnit) {
        val dataRequest = createReadRequest(dataType, startAt, endAt, bucket)
        GoogleSignIn.getLastSignedInAccount(activity)?.let {
            Fitness.getHistoryClient(activity, it)
                    .readData(dataRequest)
                    .addOnCompleteListener {
                        result.success("100")
                        // TODO add serializer for sending it to dart interface.
                    }
        }
        if (GoogleSignIn.getLastSignedInAccount(activity) == null) {
            attemptSignIn {
                if (it) {
                    getData(result, dataType, startAt, endAt, bucket)
                } else {
                    Log.e("HFP: getData", "failed to sign in")
                }
            }
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
            Log.e("HFP: onActivityResult", "requestCode:${requestCode} resultCode: ${resultCode}")
            when (requestCode) {
                GOOGLE_FIT_PERMISSIONS_REQUEST_CODE -> {
                    deferredResult?.success(true)
                    deferredResult = null
                    true
                }
                GOOGLE_SIGN_IN_REQUEST_CODE -> {
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
            Log.e("HFP: onActivityResult", "requestCode:${requestCode} resultCode: ${resultCode}")
            deferredResult?.success(false)
            deferredResult = null
            false
        }
    }
}

data class HealthFitOption(val dataType: DataType, val permission: Int)
