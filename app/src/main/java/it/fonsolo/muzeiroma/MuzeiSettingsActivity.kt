package it.fonsolo.muzeiroma

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * Activity di configurazione lanciabile direttamente da dentro Muzei
 * (icona ingranaggio nella selezione fonte, o comando "Impostazioni" nel long-press).
 */
class MuzeiSettingsActivity : AppCompatActivity() {

    private lateinit var tvDayArtwork: TextView
    private lateinit var rgMode: RadioGroup
    private lateinit var rbDayOnly: RadioButton
    private lateinit var rbRotation: RadioButton
    private lateinit var layoutRotationsRow: LinearLayout
    private lateinit var btnRotMinus: Button
    private lateinit var tvRotationsCount: TextView
    private lateinit var btnRotPlus: Button
    private lateinit var btnApply: Button
    private var currentRotationsCount: Int = RotationSettings.DEFAULT_ROTATIONS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_muzei_settings)

        supportActionBar?.title = "Muzei Roma — Impostazioni"

        tvDayArtwork    = findViewById(R.id.ms_tv_day_artwork)
        rgMode          = findViewById(R.id.ms_rg_mode)
        rbDayOnly       = findViewById(R.id.ms_rb_day_only)
        rbRotation      = findViewById(R.id.ms_rb_rotation)
        layoutRotationsRow = findViewById(R.id.ms_layout_rotations_row)
        btnRotMinus     = findViewById(R.id.ms_btn_rot_minus)
        tvRotationsCount = findViewById(R.id.ms_tv_rotations_count)
        btnRotPlus      = findViewById(R.id.ms_btn_rot_plus)
        btnApply        = findViewById(R.id.ms_btn_apply)

        loadSettings()
        showDayArtworkInfo()

        rgMode.setOnCheckedChangeListener { _, checkedId ->
            layoutRotationsRow.visibility =
                if (checkedId == R.id.ms_rb_rotation) android.view.View.VISIBLE
                else android.view.View.GONE
        }

        btnRotMinus.setOnClickListener {
            if (currentRotationsCount > 1) {
                currentRotationsCount--
                tvRotationsCount.text = currentRotationsCount.toString()
            }
        }
        btnRotPlus.setOnClickListener {
            if (currentRotationsCount < 10) {
                currentRotationsCount++
                tvRotationsCount.text = currentRotationsCount.toString()
            }
        }

        btnApply.setOnClickListener {
            saveSettings()
            UpdateWorker.runOnceNow(this)
            Toast.makeText(this, "Impostazioni salvate. Muzei si aggiornerà a breve.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadSettings() {
        val mode = RotationSettings.getMode(this)
        currentRotationsCount = RotationSettings.getRotationsCount(this)

        if (mode == RotationSettings.MODE_DAY_ONLY) {
            rbDayOnly.isChecked = true
            layoutRotationsRow.visibility = android.view.View.GONE
        } else {
            rbRotation.isChecked = true
            layoutRotationsRow.visibility = android.view.View.VISIBLE
        }
        tvRotationsCount.text = currentRotationsCount.toString()
    }

    private fun saveSettings() {
        val mode = if (rbDayOnly.isChecked) RotationSettings.MODE_DAY_ONLY else RotationSettings.MODE_ROTATION
        RotationSettings.saveSettings(this, mode, currentRotationsCount)
    }

    private fun showDayArtworkInfo() {
        val giorno = RotationSettings.getTodayGiorno()
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@MuzeiSettingsActivity)
            val artwork = db.artworkDao().getArtworkByDay(giorno)
            tvDayArtwork.text = if (artwork != null) {
                "Opera del giorno (GIORNO $giorno):\n${artwork.title}"
            } else {
                "GIORNO $giorno — archivio non ancora pronto"
            }
        }
    }
}
