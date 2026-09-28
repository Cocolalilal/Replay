package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxrave.simpmusic.ui.theme.itemSubtitleFontFamily
import com.maxrave.simpmusic.ui.theme.itemTitleFontFamily

@Composable
fun ReplayConfirmationDialog(
    title: String,
    message: String,
    confirmText: String = "Delete",
    isDestructive: Boolean = true,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontFamily = itemTitleFontFamily(),
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 18.sp,
            )
        },
        text = {
            Text(
                text = message,
                fontFamily = itemSubtitleFontFamily(),
                color = Color(0xFFAAAAAA),
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
            ) {
                Text(
                    text = confirmText,
                    fontFamily = itemTitleFontFamily(),
                    fontWeight = FontWeight.Bold,
                    color = if (isDestructive) Color(0xFFFF453A) else Color(0xFF8BA7C4),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    fontFamily = itemTitleFontFamily(),
                    color = Color.White,
                )
            }
        },
        containerColor = Color(0xFF1E1E22),
        shape = RoundedCornerShape(16.dp),
    )
}
