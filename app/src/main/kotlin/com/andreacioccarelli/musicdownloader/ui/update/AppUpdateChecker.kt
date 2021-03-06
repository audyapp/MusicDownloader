package com.andreacioccarelli.musicdownloader.ui.update

import android.app.Activity
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.checkbox.checkBoxPrompt
import com.afollestad.materialdialogs.checkbox.isCheckPromptChecked
import com.andreacioccarelli.logkit.logw
import com.andreacioccarelli.musicdownloader.App
import com.andreacioccarelli.musicdownloader.BuildConfig
import com.andreacioccarelli.musicdownloader.R
import com.andreacioccarelli.musicdownloader.constants.APK_URL
import com.andreacioccarelli.musicdownloader.constants.Keys
import com.andreacioccarelli.musicdownloader.data.requests.UpdateRequestBuilder
import com.andreacioccarelli.musicdownloader.data.serializers.UpdateCheck
import com.andreacioccarelli.musicdownloader.extensions.onceEvery4Times
import com.andreacioccarelli.musicdownloader.ui.gradients.GradientGenerator
import com.andreacioccarelli.musicdownloader.util.UpdateUtil
import com.google.gson.Gson
import com.tapadoo.alerter.Alerter
import kotlinx.coroutines.*
import okhttp3.OkHttpClient

/**
 * Designed and Developed by Andrea Cioccarelli
 */

object AppUpdateChecker {
    private val exceptionHandler = CoroutineExceptionHandler { _, _ ->
        logw("Device offline, cannot check for updates right now")
    }

    private val onPackageDownloadedReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (id < 0) return

            try {
                Alerter.hide()
            } catch (ignored: Exception) {}

            GlobalScope.launch(Dispatchers.IO) {
                delay(107)
                UpdateUtil.getDownloadedPackageFile(BuildConfig.VERSION_NAME).delete()
                UpdateUtil.openUpdateInPackageManager(context)
            }
        }
    }

    fun checkForUpdates(
        activity: Activity
    ) = onceEvery4Times {
        GlobalScope.launch(Dispatchers.IO + exceptionHandler) {
            val requestBuilder = UpdateRequestBuilder.get()
            val request = OkHttpClient().newCall(requestBuilder).execute()

            val jsonRequest = request.body!!.string()

            val check = Gson().fromJson(
                    jsonRequest,
                    UpdateCheck::class.java
            )

            App.prefs.put(Keys.lastVersionName, check.versionName)

            val isValidUpdate = check.versionCode > BuildConfig.VERSION_CODE
            val isIgnored = App.prefs.get(Keys.ignoring + check.versionCode, false)

            if (isValidUpdate && !isIgnored) {
                displayUpdateFoundDialog(activity, check)
            }
        }
    }

    private suspend fun displayUpdateFoundDialog(
        activity: Activity,
        check: UpdateCheck
    ) {
        val isUpdateAlreadyDownloaded = UpdateUtil.getDownloadedPackageFile(check.versionName).exists()

        withContext(Dispatchers.Main) {
            MaterialDialog(activity).show {
                title(text = "Update found (${check.versionName})")
                message(text = check.changelog)
                positiveButton(text = if (isUpdateAlreadyDownloaded) "INSTALL UPDATE" else "DOWNLOAD UPDATE") { dialog ->
                    if (isUpdateAlreadyDownloaded) {
                        UpdateUtil.openUpdateInPackageManager(activity)
                        dialog.dismiss()
                    } else {
                        // Downloading update package
                        val uri = Uri.parse(
                                if (check.downloadInfo.useBundledUpdateLink) APK_URL
                                else check.downloadInfo.updateLink!!
                        )

                        val downloadRequest = DownloadManager.Request(uri)

                        with(downloadRequest) {
                            setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
                            setAllowedOverRoaming(true)
                            setVisibleInDownloadsUi(true)
                            setAllowedOverMetered(true)

                            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

                            setTitle(UpdateUtil.getNotificationTitle(check))
                            setDescription(UpdateUtil.getNotificationContent())

                            setDestinationInExternalPublicDir("", UpdateUtil.getDestinationSubpath(check))
                        }

                        val downloadManager = activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                        downloadManager.enqueue(downloadRequest)

                        activity.registerReceiver(onPackageDownloadedReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
                        dialog.dismiss()

                        Alerter.create(activity)
                                .setTitle(UpdateUtil.getNotificationContent())
                                .setText(UpdateUtil.getNotificationTitle(check))
                                .setBackgroundDrawable(GradientGenerator.successGradient)
                                .setIcon(R.drawable.download)
                                .setDuration(9_000)
                                .setDismissable(false)
                                .show()
                    }
                }
                negativeButton(text = "NO") { dialog ->
                    if (dialog.isCheckPromptChecked() && isUpdateAlreadyDownloaded) {
                        UpdateUtil.clearDuplicatedInstallationPackage("music-downloader-${check.versionName}.apk")
                    }
                    dialog.dismiss()
                }
                checkBoxPrompt(text = "Ignore this update", isCheckedDefault = false) { state ->
                    App.prefs.put(Keys.ignoring + check.versionCode, state)
                }
                cancelable(false)
                noAutoDismiss()
            }
        }
    }
}