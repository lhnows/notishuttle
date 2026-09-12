package com.notishuttle

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.notishuttle.config.AppSettings
import com.notishuttle.config.FilterMode
import com.notishuttle.databinding.ActivityMainBinding
import com.notishuttle.listener.NotificationRelayService
import com.notishuttle.model.AppInfo
import com.notishuttle.model.NotificationInfo
import com.notishuttle.model.NotificationPayload
import com.notishuttle.model.TimeInfo
import com.notishuttle.model.toJson
import com.notishuttle.util.DeviceInfoBuilder
import com.notishuttle.webhook.WebhookSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var settings: AppSettings

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        settings = AppSettings.get(this)

        loadSettings()
        setupListeners()
        maybeRequestNotificationPermission()
    }

    override fun onResume() {
        super.onResume()
        refreshAccessStatus()
    }

    private fun loadSettings() {
        binding.editUrl.setText(settings.webhookUrl)
        binding.switchEnabled.isChecked = settings.enabled
        binding.switchIgnoreOngoing.isChecked = settings.ignoreOngoing
        binding.switchIgnoreSilent.isChecked = settings.ignoreSilent
        binding.editKeyword.setText(settings.keyword)
        binding.editSecret.setText(settings.secret)
        binding.editHeaders.setText(settings.headersJson)
        when (settings.filterMode) {
            FilterMode.ALL -> binding.rbAll.isChecked = true
            FilterMode.ALLOWLIST -> binding.rbAllowlist.isChecked = true
            FilterMode.BLOCKLIST -> binding.rbBlocklist.isChecked = true
        }
    }

    private fun setupListeners() {
        binding.switchEnabled.setOnCheckedChangeListener { _, checked ->
            settings.enabled = checked
            if (checked && !NotificationRelayService.isAccessGranted(this)) {
                openNotificationAccessSettings()
            }
        }

        binding.editUrl.doAfterTextChanged { settings.webhookUrl = it?.toString().orEmpty() }
        binding.editKeyword.doAfterTextChanged { settings.keyword = it?.toString().orEmpty() }
        binding.editSecret.doAfterTextChanged { settings.secret = it?.toString().orEmpty() }
        binding.editHeaders.doAfterTextChanged { settings.headersJson = it?.toString().orEmpty() }

        binding.switchIgnoreOngoing.setOnCheckedChangeListener { _, checked ->
            settings.ignoreOngoing = checked
        }
        binding.switchIgnoreSilent.setOnCheckedChangeListener { _, checked ->
            settings.ignoreSilent = checked
        }

        binding.rgFilter.setOnCheckedChangeListener { _, checkedId ->
            settings.filterMode = when (checkedId) {
                R.id.rbAllowlist -> FilterMode.ALLOWLIST
                R.id.rbBlocklist -> FilterMode.BLOCKLIST
                else -> FilterMode.ALL
            }
        }

        binding.btnAccess.setOnClickListener { openNotificationAccessSettings() }
        binding.btnManageApps.setOnClickListener {
            startActivity(Intent(this, AppFilterActivity::class.java))
        }
        binding.btnTest.setOnClickListener { sendTest() }
        binding.btnBattery.setOnClickListener { requestIgnoreBatteryOptimizations() }
    }

    private fun refreshAccessStatus() {
        val granted = NotificationRelayService.isAccessGranted(this)
        binding.tvAccessStatus.text = getString(
            if (granted) R.string.access_status_granted else R.string.access_status_denied
        )
        binding.tvAccessStatus.setTextColor(
            ContextCompat.getColor(this, if (granted) R.color.ok_green else R.color.err_red)
        )
    }

    private fun openNotificationAccessSettings() {
        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    private fun sendTest() {
        val url = binding.editUrl.text?.toString()?.trim().orEmpty()
        if (url.isBlank()) {
            binding.tilUrl.error = getString(R.string.hint_webhook)
            return
        }
        binding.tilUrl.error = null
        settings.webhookUrl = url

        val payload = buildTestPayload()
        binding.tvTestResult.text = getString(R.string.test_sending)
        lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) {
                WebhookSender.send(url, payload.toJson(), settings.secret, settings.headersJson)
            }
            binding.tvTestResult.text = getString(
                if (ok) R.string.test_result_ok else R.string.test_result_fail
            )
            binding.tvTestResult.setTextColor(
                ContextCompat.getColor(this@MainActivity, if (ok) R.color.ok_green else R.color.err_red)
            )
        }
    }

    private fun buildTestPayload(): NotificationPayload = NotificationPayload(
        id = -1,
        key = null,
        tag = "test",
        app = AppInfo(
            packageName = BuildConfig.APPLICATION_ID,
            label = getString(R.string.app_name),
            category = "test",
            channelId = "test",
        ),
        notification = NotificationInfo(
            title = "NotiShuttle test",
            text = "If you can read this, your webhook is working.",
            bigText = null,
            subText = null,
            tickerText = null,
            isOngoing = false,
            isClearable = true,
            groupKey = null,
            actions = null,
        ),
        time = TimeInfo(postTime = System.currentTimeMillis(), `when` = System.currentTimeMillis()),
        device = DeviceInfoBuilder.build(this, settings),
    )

    private fun requestIgnoreBatteryOptimizations() {
        val pm = getSystemService(PowerManager::class.java)
        if (pm.isIgnoringBatteryOptimizations(packageName)) {
            Toast.makeText(this, R.string.battery_exempt, Toast.LENGTH_SHORT).show()
            return
        }
        runCatching {
            startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    .setData(Uri.parse("package:$packageName"))
            )
        }
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
