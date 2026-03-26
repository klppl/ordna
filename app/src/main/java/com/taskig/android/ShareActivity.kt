package com.taskig.android

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.taskig.android.data.remote.GoogleTasksApi
import com.taskig.android.data.repository.SettingsRepository
import com.taskig.android.data.repository.TaskRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ShareActivity : ComponentActivity() {

    @Inject lateinit var api: GoogleTasksApi
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var taskRepository: TaskRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent?.action != Intent.ACTION_SEND || intent.type?.startsWith("text/") != true) {
            finish()
            return
        }

        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
        val sharedSubject = intent.getStringExtra(Intent.EXTRA_SUBJECT)

        if (sharedText.isNullOrBlank() && sharedSubject.isNullOrBlank()) {
            Toast.makeText(this, R.string.share_no_content, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Build the task title from subject + text
        val title = buildString {
            if (!sharedSubject.isNullOrBlank()) {
                append(sharedSubject)
                if (!sharedText.isNullOrBlank() && sharedText != sharedSubject) {
                    append(" ")
                    append(sharedText)
                }
            } else {
                append(sharedText)
            }
        }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val email = taskRepository.getAccountEmail()
                if (email == null) {
                    Toast.makeText(this@ShareActivity, R.string.share_failed, Toast.LENGTH_SHORT).show()
                    finish()
                    return@launch
                }

                // Get configured share list, or fall back to first available list
                var listId = settingsRepository.getShareListId()
                var listTitle = settingsRepository.getShareListTitle()

                if (listId == null) {
                    val lists = api.fetchTaskLists(email)
                    val firstList = lists.firstOrNull()
                    if (firstList?.id != null) {
                        listId = firstList.id
                        listTitle = firstList.title ?: "Tasks"
                        settingsRepository.setShareList(listId, listTitle)
                    } else {
                        Toast.makeText(this@ShareActivity, R.string.share_failed, Toast.LENGTH_SHORT).show()
                        finish()
                        return@launch
                    }
                }

                api.createTask(email, listId, title)
                Toast.makeText(
                    this@ShareActivity,
                    getString(R.string.share_added_to, listTitle),
                    Toast.LENGTH_SHORT,
                ).show()
            } catch (_: Exception) {
                Toast.makeText(this@ShareActivity, R.string.share_failed, Toast.LENGTH_SHORT).show()
            }
            finish()
        }
    }
}
