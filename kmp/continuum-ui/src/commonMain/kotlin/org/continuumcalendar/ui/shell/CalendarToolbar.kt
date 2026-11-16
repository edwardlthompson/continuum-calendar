package org.continuumcalendar.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.continuumcalendar.ui.theme.LocalContinuumPalette

@Composable
fun CalendarToolbar(
    view: ContinuumMainView,
    searchQuery: String,
    goToText: String,
    onSelectView: (ContinuumMainView) -> Unit,
    onSearchChange: (String) -> Unit,
    onGoToChange: (String) -> Unit,
    onGoToSubmit: () -> Unit,
    onToday: () -> Unit,
    searchFocusRequester: FocusRequester? = null,
    goToFocusRequester: FocusRequester? = null,
    requestSearchFocus: Boolean = false,
    requestGoToFocus: Boolean = false,
) {
    val palette = LocalContinuumPalette.current
    LaunchedEffect(requestSearchFocus) {
        if (requestSearchFocus) searchFocusRequester?.requestFocus()
    }
    LaunchedEffect(requestGoToFocus) {
        if (requestGoToFocus) goToFocusRequester?.requestFocus()
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(palette.surface)
            .border(1.dp, palette.border, RoundedCornerShape(4.dp))
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf(
            ContinuumMainView.AGENDA to "Agenda",
            ContinuumMainView.ROLLING to "Week",
            ContinuumMainView.MONTH to "Month",
            ContinuumMainView.YEAR to "Year",
        ).forEach { (v, label) ->
            val selected = view == v
            TextButton(onClick = { onSelectView(v) }) {
                Text(
                    text = label,
                    color = if (selected) palette.accent else palette.muted,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                )
            }
        }
        Spacer(Modifier.weight(1f))
        ToolbarField(
            value = searchQuery,
            placeholder = "Search",
            onChange = onSearchChange,
            modifier = Modifier.widthIn(min = 120.dp, max = 180.dp),
            focusRequester = searchFocusRequester,
        )
        ToolbarField(
            value = goToText,
            placeholder = "YYYY-MM-DD",
            onChange = onGoToChange,
            modifier = Modifier.width(120.dp),
            focusRequester = goToFocusRequester,
        )
        TextButton(onClick = onGoToSubmit) {
            Text("Go", color = palette.accent)
        }
        TextButton(onClick = onToday) {
            Text("Today", color = palette.accent)
        }
    }
}

@Composable
private fun ToolbarField(
    value: String,
    placeholder: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
) {
    val palette = LocalContinuumPalette.current
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodySmall.copy(color = palette.text),
        cursorBrush = SolidColor(palette.accent),
        modifier = modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .clip(RoundedCornerShape(4.dp))
            .background(palette.background)
            .border(1.dp, palette.border, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        decorationBox = { inner ->
            if (value.isEmpty()) {
                Text(placeholder, color = palette.muted, style = MaterialTheme.typography.bodySmall)
            }
            inner()
        },
    )
}
