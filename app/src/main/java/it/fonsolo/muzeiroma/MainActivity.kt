package it.fonsolo.muzeiroma

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var btnDownload: Button
    private lateinit var btnClearLogs: Button
    private lateinit var rvLogs: RecyclerView
    private lateinit var rgLanguage: RadioGroup
    private lateinit var rbLangIt: RadioButton
    private lateinit var rbLangEn: RadioButton
    private lateinit var rbLangFr: RadioButton
    private lateinit var tvSiteLink: TextView
    private lateinit var tvRepoLink: TextView
    private val logAdapter = LogAdapter()

    companion object {
        const val PREF_LANGUAGE = "language"
        const val URL_SITE = "https://www.aibofobia.net/wrm/"
        const val URL_REPO = "https://github.com/fonsolino/muzei-roma/"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus      = findViewById(R.id.tv_archive_status)
        btnDownload   = findViewById(R.id.btn_download_more)
        btnClearLogs  = findViewById(R.id.btn_clear_logs)
        rvLogs        = findViewById(R.id.rv_logs)
        rgLanguage    = findViewById(R.id.rg_language)
        rbLangIt      = findViewById(R.id.rb_lang_it)
        rbLangEn      = findViewById(R.id.rb_lang_en)
        rbLangFr      = findViewById(R.id.rb_lang_fr)
        tvSiteLink    = findViewById(R.id.tv_site_link)
        tvRepoLink    = findViewById(R.id.tv_repo_link)

        rvLogs.layoutManager = LinearLayoutManager(this)
        rvLogs.adapter = logAdapter

        UpdateWorker.schedule(this)

        // Selezione lingua corrente
        val currentLang = getCurrentLanguageTag()
        when (currentLang) {
            "en" -> rbLangEn.isChecked = true
            "fr" -> rbLangFr.isChecked = true
            else -> rbLangIt.isChecked = true
        }

        rgLanguage.setOnCheckedChangeListener { _, checkedId ->
            val tag = when (checkedId) {
                R.id.rb_lang_en -> "en"
                R.id.rb_lang_fr -> "fr"
                else -> "it"
            }
            if (tag != currentLang) {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
            }
        }

        tvSiteLink.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(URL_SITE)))
        }
        tvRepoLink.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(URL_REPO)))
        }

        btnDownload.setOnClickListener {
            btnDownload.isEnabled = false
            UpdateWorker.runOnceNow(this)
        }

        btnClearLogs.setOnClickListener {
            lifecycleScope.launch {
                AppDatabase.getDatabase(this@MainActivity).logDao().clearLogs()
            }
        }

        val db = AppDatabase.getDatabase(this)
        observeArchiveStatus(db)
        observeLogs(db)
    }

    private fun getCurrentLanguageTag(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        if (!locales.isEmpty) {
            val tag = locales[0]?.language ?: "it"
            return tag
        }
        return resources.configuration.locales[0]?.language ?: "it"
    }

    private fun observeArchiveStatus(db: AppDatabase) {
        lifecycleScope.launch {
            combine(
                db.artworkDao().getAllCountFlow(),
                db.artworkDao().getDownloadedCountFlow()
            ) { total, downloaded -> Pair(total, downloaded) }
                .collect { (total, downloaded) ->
                    if (total > 0 && downloaded >= total) {
                        tvStatus.text = getString(R.string.archive_synced, downloaded, total)
                        btnDownload.text = getString(R.string.btn_synced)
                        btnDownload.isEnabled = false
                    } else {
                        tvStatus.text = getString(R.string.archive_downloading, downloaded, total)
                        btnDownload.text = getString(R.string.btn_download)
                        btnDownload.isEnabled = total > 0
                    }
                }
        }
    }

    private fun observeLogs(db: AppDatabase) {
        lifecycleScope.launch {
            db.logDao().getAllLogs().collect { logs ->
                logAdapter.submitList(logs)
                if (logs.isNotEmpty()) {
                    rvLogs.scrollToPosition(0)
                }
            }
        }
    }
}
