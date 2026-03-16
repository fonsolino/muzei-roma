package it.fonsolo.muzeiroma

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var btnDownload: Button
    private lateinit var btnClearLogs: Button
    private lateinit var rvLogs: RecyclerView
    private val logAdapter = LogAdapter()
    private var totalArtworks: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tv_archive_status)
        btnDownload = findViewById(R.id.btn_download_more)
        btnClearLogs = findViewById(R.id.btn_clear_logs)
        rvLogs = findViewById(R.id.rv_logs)

        rvLogs.layoutManager = LinearLayoutManager(this)
        rvLogs.adapter = logAdapter

        UpdateWorker.schedule(this)

        btnDownload.setOnClickListener {
            btnDownload.isEnabled = false
            tvStatus.text = "Operazione avviata..."
            UpdateWorker.runOnceNow(this)
        }

        btnClearLogs.setOnClickListener {
            lifecycleScope.launch {
                AppDatabase.getDatabase(this@MainActivity).logDao().clearLogs()
            }
        }

        val db = AppDatabase.getDatabase(this)
        lifecycleScope.launch {
            totalArtworks = db.artworkDao().getCount()
            observeArchiveStatus(db)
        }
        observeLogs(db)
    }

    private fun observeArchiveStatus(db: AppDatabase) {
        lifecycleScope.launch {
            db.artworkDao().getDownloadedCountFlow().collect { count ->
                if (count >= totalArtworks && totalArtworks > 0) {
                    tvStatus.text = "Archivio Sincronizzato ($count / $totalArtworks)"
                    btnDownload.text = "Sincronizzato"
                    btnDownload.isEnabled = false
                } else {
                    tvStatus.text = "Opere scaricate: $count / $totalArtworks"
                    btnDownload.text = "scarica altre"
                    btnDownload.isEnabled = true
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
