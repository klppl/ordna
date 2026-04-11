package io.github.klppl.ordna.screenshots

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.klppl.ordna.data.local.TaskEntity

@Composable
fun CounterWidgetDemo(
    remaining: Int,
    totalCount: Int,
    completedCount: Int,
    streak: Int,
    accentColor: Color,
    bgColor: Color,
    textColor: Color,
    subtextColor: Color,
    trackColor: Color,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor),
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(accentColor),
            )
            Column(
                modifier = Modifier.padding(start = 12.dp, top = 10.dp, end = 12.dp, bottom = 10.dp),
            ) {
                if (remaining > 0) {
                    Text(
                        text = "$remaining",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                    )
                } else {
                    Text(
                        text = "All done",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                    )
                    if (streak > 0) {
                        Text(
                            text = "$streak-day streak",
                            fontSize = 11.sp,
                            color = subtextColor,
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                LinearProgressIndicator(
                    progress = { if (totalCount > 0) completedCount.toFloat() / totalCount else 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = accentColor,
                    trackColor = trackColor,
                )
            }
        }
    }
}

@Composable
fun OrdnaWidgetDemo(
    overdueTasks: List<TaskEntity>,
    todayTasks: List<TaskEntity>,
    completedTasks: List<TaskEntity>,
    completedCount: Int,
    totalCount: Int,
    streak: Int,
    accentColor: Color,
    bgColor: Color,
    textColor: Color,
    subtextColor: Color,
    overdueColor: Color,
    completedColor: Color,
    trackColor: Color,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "$completedCount/$totalCount",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                )
                Spacer(modifier = Modifier.weight(1f))
                if (streak > 0) {
                    Text(
                        text = "\uD83D\uDD25 $streak",
                        fontSize = 12.sp,
                        color = subtextColor,
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { if (totalCount > 0) completedCount.toFloat() / totalCount else 0f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = accentColor,
                trackColor = trackColor,
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Overdue section
            if (overdueTasks.isNotEmpty()) {
                Text(
                    text = "OVERDUE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = overdueColor,
                    letterSpacing = 1.sp,
                )
                overdueTasks.forEach { task ->
                    WidgetTaskRowDemo(task = task, textColor = overdueColor)
                }
            }

            // Today section
            todayTasks.forEach { task ->
                WidgetTaskRowDemo(task = task, textColor = textColor)
            }

            // Completed section
            if (completedTasks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "COMPLETED",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = completedColor,
                    letterSpacing = 1.sp,
                )
                completedTasks.forEach { task ->
                    WidgetTaskRowDemo(
                        task = task,
                        textColor = completedColor.copy(alpha = 0.6f),
                        strikethrough = true,
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetTaskRowDemo(
    task: TaskEntity,
    textColor: Color,
    strikethrough: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(Color(task.listColor), RoundedCornerShape(3.dp)),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = task.title,
            fontSize = 13.sp,
            color = textColor,
            textDecoration = if (strikethrough) TextDecoration.LineThrough else TextDecoration.None,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = task.listTitle,
            fontSize = 10.sp,
            color = Color(task.listColor),
        )
    }
}
