package com.notishuttle

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.notishuttle.config.AppSettings
import com.notishuttle.databinding.ActivityAppFilterBinding

data class AppItem(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
)

class AppFilterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppFilterBinding
    private lateinit var settings: AppSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppFilterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        settings = AppSettings.get(this)

        val apps = loadApps()
        val adapter = AppListAdapter(apps, settings.filteredApps) { pkg, selected ->
            val current = settings.filteredApps.toMutableSet()
            if (selected) current.add(pkg) else current.remove(pkg)
            settings.filteredApps = current
        }

        binding.recycler.layoutManager = LinearLayoutManager(this)
        binding.recycler.adapter = adapter
        binding.tvEmpty.visibility = if (apps.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun loadApps(): List<AppItem> {
        val pm = packageManager
        return pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .mapNotNull { info ->
                if (info.packageName == BuildConfig.APPLICATION_ID) return@mapNotNull null
                val label = runCatching { info.loadLabel(pm).toString() }.getOrNull()
                    ?: return@mapNotNull null
                AppItem(
                    packageName = info.packageName,
                    label = label,
                    icon = runCatching { info.loadIcon(pm) }.getOrNull(),
                )
            }
            .sortedBy { it.label.lowercase() }
    }
}
