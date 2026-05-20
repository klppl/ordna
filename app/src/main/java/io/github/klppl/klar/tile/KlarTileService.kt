package io.github.klppl.klar.tile

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import io.github.klppl.klar.MainActivity
import io.github.klppl.klar.R
import io.github.klppl.klar.data.local.TaskDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Quick Settings tile showing the number of tasks due today. Refreshes each
 * time the QS shade opens (onStartListening). Tapping opens the app.
 */
class KlarTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onStartListening() {
        super.onStartListening()
        scope.launch {
            val count = withContext(Dispatchers.IO) {
                TaskDatabase.getInstance(applicationContext).taskDao()
                    .getActiveTaskCount(LocalDate.now())
            }
            val tile = qsTile ?: return@launch
            val status = if (count == 0) {
                getString(R.string.tile_all_done)
            } else {
                resources.getQuantityString(R.plurals.tile_due, count, count)
            }
            tile.state = if (count == 0) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
            // Subtitle exists on API 29+; older devices show the status in the label.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.label = getString(R.string.tile_label)
                tile.subtitle = status
            } else {
                tile.label = status
            }
            tile.updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION", "StartActivityAndCollapseDeprecated")
            startActivityAndCollapse(intent)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
