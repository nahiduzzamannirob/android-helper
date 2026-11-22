package com.example.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.model.ActiveWarning
import com.example.data.model.WarningSeverity
import com.example.ui.theme.VoltAmber
import com.example.ui.theme.VoltCrimson
import com.example.ui.theme.VoltCyan

@Composable
fun WarningBannerCard(
  warning: ActiveWarning,
  onOpenSettings: () -> Unit,
  onDismissTest: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val accent =
    when (warning.severity) {
      WarningSeverity.CRITICAL -> VoltCrimson
      WarningSeverity.WARNING -> VoltAmber
      WarningSeverity.INFO -> VoltCyan
    }

  val icon =
    when (warning.severity) {
      WarningSeverity.CRITICAL -> Icons.Rounded.ErrorOutline
      WarningSeverity.WARNING -> Icons.Rounded.WarningAmber
      WarningSeverity.INFO -> Icons.Rounded.Info
    }

  Surface(
    modifier =
      modifier
        .fillMaxWidth()
        .border(1.dp, accent.copy(alpha = 0.5f), MaterialTheme.shapes.medium)
        .clickable(onClick = onOpenSettings),
    shape = MaterialTheme.shapes.medium,
    color = accent.copy(alpha = 0.12f),
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      verticalAlignment = Alignment.Top,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = warning.title,
        tint = accent,
        modifier = Modifier.size(22.dp).padding(top = 2.dp),
      )
      Column(modifier = Modifier.weight(1f)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = warning.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
          )
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = accent.copy(alpha = 0.2f),
          ) {
            Text(
              text = warning.metricLabel,
              style = MaterialTheme.typography.labelMedium,
              color = accent,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            )
          }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = warning.message,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      if (onDismissTest != null) {
        IconButton(onClick = onDismissTest, modifier = Modifier.size(32.dp)) {
          Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = "Dismiss preview warning",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
          )
        }
      }
    }
  }
}
