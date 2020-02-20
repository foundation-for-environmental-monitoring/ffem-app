package org.akvo.caddisfly.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.View
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import kotlinx.android.synthetic.main.activity_main.*
import org.akvo.caddisfly.BuildConfig
import org.akvo.caddisfly.R
import org.akvo.caddisfly.app.CaddisflyApp
import org.akvo.caddisfly.common.AppConfig
import org.akvo.caddisfly.common.ConstantKey
import org.akvo.caddisfly.common.Constants
import org.akvo.caddisfly.common.NavigationController
import org.akvo.caddisfly.helper.ApkHelper
import org.akvo.caddisfly.helper.ErrorMessages
import org.akvo.caddisfly.helper.FileHelper
import org.akvo.caddisfly.helper.PermissionsDelegate
import org.akvo.caddisfly.model.TestSampleType
import org.akvo.caddisfly.model.TestType
import org.akvo.caddisfly.preference.AppPreferences
import org.akvo.caddisfly.preference.SettingsActivity
import org.akvo.caddisfly.util.AlertUtil
import org.akvo.caddisfly.util.PreferencesUtil
import org.akvo.caddisfly.viewmodel.TestListViewModel
import java.lang.ref.WeakReference
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

const val STORAGE_PERMISSION_WATER = 1
const val STORAGE_PERMISSION_SOIL = 2

class MainActivity : BaseActivity() {
    private val refreshHandler = WeakRefHandler(this)
    private val permissionsDelegate = PermissionsDelegate(this)
    private val storagePermission = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    private var navigationController: NavigationController? = null
    private var runTest = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CaddisflyApp.app?.setAppLanguage(null, false, null)
        navigationController = NavigationController(this)
        setContentView(R.layout.activity_main)
        setTitle(R.string.appName)
        try {
            if (AppConfig.APP_EXPIRY && ApkHelper.isNonStoreVersion(this)) {
                val appExpiryDate = GregorianCalendar(AppConfig.APP_EXPIRY_YEAR,
                        AppConfig.APP_EXPIRY_MONTH - 1, AppConfig.APP_EXPIRY_DAY)
                val df: DateFormat = SimpleDateFormat("dd-MMM-yyyy", Locale.US)
                textVersionExpiry?.text = String.format("Version expiry: %s", df.format(appExpiryDate.time))
                textVersionExpiry?.visibility = View.VISIBLE
            } else {
                @Suppress("ConstantConditionIf")
                if (BuildConfig.showExperimentalTests) {
                    textVersionExpiry?.text = CaddisflyApp.getAppVersion(true)
                    textVersionExpiry?.visibility = View.VISIBLE
                }
            }
            // If app has expired then close this activity
            ApkHelper.isAppVersionExpired(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        buttonCalibrate?.setOnClickListener {
            if (permissionsDelegate.hasPermissions(storagePermission)) {
                startCalibrate(TestSampleType.ALL)
            } else {
                if (BuildConfig.APPLICATION_ID.contains("soil")) {
                    permissionsDelegate.requestPermissions(storagePermission, STORAGE_PERMISSION_SOIL)
                } else {
                    permissionsDelegate.requestPermissions(storagePermission, STORAGE_PERMISSION_WATER)
                }
            }
        }
        buttonSettings?.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivityForResult(intent, 100)
        }
    }

    /**
     * Show the diagnostic mode layout.
     */
    private fun switchLayoutForDiagnosticOrUserMode() {
        if (AppPreferences.isDiagnosticMode()) {
            textDiagnostics.visibility = View.VISIBLE
        } else {
            textDiagnostics.visibility = View.GONE
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(false)
        }
    }

    override fun onResume() {
        super.onResume()
        switchLayoutForDiagnosticOrUserMode()
        CaddisflyApp.app?.setAppLanguage(null, false, refreshHandler)
        if (PreferencesUtil.getBoolean(this, R.string.themeChangedKey, false)) {
            PreferencesUtil.setBoolean(this, R.string.themeChangedKey, false)
            refreshHandler.sendEmptyMessage(0)
        }
    }

    fun onDisableDiagnosticsClick(@Suppress("UNUSED_PARAMETER") view: View?) {
        Toast.makeText(this, getString(R.string.diagnosticModeDisabled),
                Toast.LENGTH_SHORT).show()
        AppPreferences.disableDiagnosticMode()
        switchLayoutForDiagnosticOrUserMode()
        changeActionBarStyleBasedOnCurrentMode()
        val viewModel = ViewModelProvider(this).get(TestListViewModel::class.java)
        viewModel.clearTests()
    }

    fun onStripTestsClick(@Suppress("UNUSED_PARAMETER") view: View?) {
        navigationController!!.navigateToTestType(TestType.STRIP_TEST, TestSampleType.ALL, true)
    }

    fun onSensorsClick(@Suppress("UNUSED_PARAMETER") view: View?) {
        val hasOtg = packageManager.hasSystemFeature(PackageManager.FEATURE_USB_HOST)
        if (hasOtg) {
            navigationController!!.navigateToTestType(TestType.SENSOR, TestSampleType.ALL, true)
        } else {
            ErrorMessages.alertFeatureNotSupported(this, false)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (permissionsDelegate.resultGranted(requestCode, grantResults)) {
            when (requestCode) {
                STORAGE_PERMISSION_WATER -> if (runTest) {
                    startTest(TestSampleType.WATER)
                } else {
                    startCalibrate(TestSampleType.WATER)
                }
                STORAGE_PERMISSION_SOIL -> if (runTest) {
                    startTest(TestSampleType.SOIL)
                } else {
                    startCalibrate(TestSampleType.SOIL)
                }
            }
        } else {
            val message = ""
            when (requestCode) {
                STORAGE_PERMISSION_WATER, STORAGE_PERMISSION_SOIL -> {
                }
            }
            AlertUtil.showSettingsSnackbar(this,
                    window.decorView.rootView, message)
        }
    }

    private fun startCalibrate(testSampleType: TestSampleType) {
        FileHelper.migrateFolders()
        navigationController!!.navigateToTestType(TestType.CHAMBER_TEST, testSampleType, false)
    }

    fun onSettingsClick(@Suppress("UNUSED_PARAMETER") view: View?) {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivityForResult(intent, 100)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && PreferencesUtil.getBoolean(this, R.string.refreshKey, false)) {
            PreferencesUtil.setBoolean(this, R.string.refreshKey, false)
            recreate()
        }
    }

    fun onColiformsClick(@Suppress("UNUSED_PARAMETER") view: View?) {
        val viewModel = ViewModelProvider(this).get(TestListViewModel::class.java)
        val testInfo = viewModel.getTestInfo(Constants.COLIFORM_ID)
        val intent = Intent(this, TestActivity::class.java)
        intent.putExtra(ConstantKey.TEST_INFO, testInfo)
        startActivity(intent)
    }

    fun onCalibrateSoilClick(@Suppress("UNUSED_PARAMETER") view: View?) {
        runTest = false
        if (permissionsDelegate.hasPermissions(storagePermission)) {
            startCalibrate(TestSampleType.SOIL)
        } else {
            permissionsDelegate.requestPermissions(storagePermission, STORAGE_PERMISSION_SOIL)
        }
    }

    fun onCalibrateWaterClick(@Suppress("UNUSED_PARAMETER") view: View?) {
        if (permissionsDelegate.hasPermissions(storagePermission)) {
            startCalibrate(TestSampleType.WATER)
        } else {
            permissionsDelegate.requestPermissions(storagePermission, STORAGE_PERMISSION_WATER)
        }
    }

    fun onRunTestClick(@Suppress("UNUSED_PARAMETER") view: View?) {
        runTest = true
        if (permissionsDelegate.hasPermissions(storagePermission)) {
            startTest(TestSampleType.ALL)
        } else {
            if (BuildConfig.APPLICATION_ID.contains("soil")) {
                permissionsDelegate.requestPermissions(storagePermission, STORAGE_PERMISSION_SOIL)
            } else {
                permissionsDelegate.requestPermissions(storagePermission, STORAGE_PERMISSION_WATER)
            }
        }
    }

    private fun startTest(testSampleType: TestSampleType) {
        FileHelper.migrateFolders()
        navigationController!!.navigateToTestType(TestType.CHAMBER_TEST, testSampleType, runTest)
    }

    /**
     * Handler to restart the app after language has been changed.
     */
    private class WeakRefHandler internal constructor(ref: Activity) : Handler() {
        private val ref: WeakReference<Activity> = WeakReference(ref)
        override fun handleMessage(msg: Message) {
            val f = ref.get()
            f?.recreate()
        }
    }
}