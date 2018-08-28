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
import com.google.android.gms.fitness.data.DataSource
import java.util.*
import com.google.android.gms.fitness.data.DataSource.TYPE_DERIVED
import com.google.android.gms.fitness.data.Field
import java.text.SimpleDateFormat


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
        Log.d("HFP: onMethodCall", "${call.method}")
        when (call.method) {
            "getPlatformVersion" -> {
            }
            "hasPermission" -> hasPermission(call, result)
            "requestPermission" -> requestPermission(call, result)
            "disable" -> disable(result)
            "getData" -> {
                getData(result, getDataType(call), getDataField(call), getStartAt(call), getEndAt(call), getTimeUnit(call))
            }
            else -> result.notImplemented()
        }
    }

    private fun hasPermission(call: MethodCall, result: Result) {
        val healthFitOption = getHealthFitOptions(call)
        val optionsBuilder = FitnessOptions.builder()
        optionsBuilder.addDataType(healthFitOption.dataType, healthFitOption.permission)
        Log.d("HFP: hasPermission option", "${healthFitOption.dataType}:${healthFitOption.permission}")
        Log.d("HFP: hasPermission Account", "${GoogleSignIn.getLastSignedInAccount(activity)}")
        Log.d("HFP: hasPermission", "${GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(activity), optionsBuilder.build())}")
        result.success(GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(activity), optionsBuilder.build()))
    }

    private fun getHealthFitOptions(call: MethodCall): HealthFitOption =
            HealthFitOption(getDataType(call), getPermission(call))


    private fun getDataType(call: MethodCall): DataType = when (call.argument<String>("dataType")) {
        "DataType.STEP" -> DataType.TYPE_STEP_COUNT_DELTA
        else -> throw IllegalArgumentException("${call.argument<String>("dataType")} is not allowed here.")
    }

    private fun getDataField(call: MethodCall): Field = when (call.argument<String>("dataType")) {
        "DataType.STEP" -> Field.FIELD_STEPS
        else -> throw IllegalArgumentException("${call.argument<String>("dataType")} is not allowed here.")
    }

    private fun getTimeUnit(call: MethodCall): TimeUnit = when (call.argument<String>("timeUnit")) {
        "TimeUnit.MILLISECONDS" -> TimeUnit.MILLISECONDS
        "TimeUnit.HOURS" -> TimeUnit.HOURS
        else -> throw IllegalArgumentException("${call.argument<String>("timeUnit")} is not allowed here.")
    }

    private fun getStartAt(call: MethodCall): Long = call.argument<Long>("startAt")
    private fun getEndAt(call: MethodCall): Long = call.argument<Long>("endAt")

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

    private fun getData(result: Result, dataType: DataType, field: Field, startAt: Long, endAt: Long, bucket: TimeUnit) {
        Log.d("HFP: getData starTime", startAt.toString())
        Log.d("HFP: getData endTime", endAt.toString())
        val dataRequest = createReadRequest(dataType, startAt, endAt, bucket)
        GoogleSignIn.getLastSignedInAccount(activity)?.let {
            Fitness.getHistoryClient(activity, it)
                    .readData(dataRequest)
                    .addOnSuccessListener {
                        Log.d("HFP: getData success", it.getDataSet(dataType).toString())
                        val data = mutableListOf<String>()
                        for (point in it.getDataSet(dataType).dataPoints) {
                            // FIXME passing the object to the healthfit dart as object.
                            val dateFormat = SimpleDateFormat("MM月dd日 HH時mm分ss秒")
                            val dateString = dateFormat.format(Date(point.getTimestamp(bucket)))
                            data.add("$dateString: ${point.getValue(field)}")
                        }
                        result.success(data.joinToString(separator = "\n"))
                    }
                    .addOnFailureListener {
                        Log.e("HFP: getData failed", it.toString())
                    }
                    .addOnCompleteListener {
                        // TODO add serializer for sending it to dart interface.
                    }
        }
        if (GoogleSignIn.getLastSignedInAccount(activity) == null) {
            attemptSignIn {
                if (it) {
                    getData(result, dataType, field, startAt, endAt, bucket)
                } else {
                    Log.e("HFP: getData", "failed to sign in")
                }
            }
        }
    }

    private fun createReadRequest(dataType: DataType,
                                  startAt: Long,
                                  endAt: Long,
                                  bucket: TimeUnit): DataReadRequest {
        return DataReadRequest.Builder()
                .read(dataType)
                .setTimeRange(startAt, endAt, bucket)
                // TODO remove this later.
                .setLimit(30)
                .build()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent): Boolean = when (resultCode) {
        Activity.RESULT_OK -> {
            Log.d("HFP: onActivityResult", "requestCode:$requestCode resultCode: $resultCode")
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
            Log.d("HFP: onActivityResult", "requestCode:$requestCode resultCode: $resultCode")
            deferredResult?.success(false)
            deferredResult = null
            false
        }
    }

}

data class HealthFitOption(val dataType: DataType, val permission: Int)
