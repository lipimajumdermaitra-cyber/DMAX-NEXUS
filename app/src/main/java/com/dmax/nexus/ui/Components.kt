package com.dmax.nexus.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ScreenHeader(kicker: String, title: String, subtitle: String) {
    val p = LocalNexusPalette.current
    Text(kicker.uppercase(), color = p.accent, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(7.dp))
    Text(title, color = p.text, fontSize = 28.sp, lineHeight = 33.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(5.dp))
    Text(subtitle, color = p.muted, fontSize = 13.sp, lineHeight = 19.sp)
    Spacer(Modifier.height(20.dp))
}

@Composable
fun BrandHeader() {
    val p = LocalNexusPalette.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("YOUR DIGITAL UNIVERSE", color = p.accent, fontSize = 9.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.SemiBold)
            Text("DMAX NEXUS", color = p.text, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
        }
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(p.elevated)
                .border(BorderStroke(1.dp, p.border), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("DN", color = p.accent, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
        }
    }
}

@Composable
fun NexusPanel(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val p = LocalNexusPalette.current
    Surface(
        modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(20.dp),
        color = p.surface,
        border = BorderStroke(1.dp, p.border)
    ) {
        Box(Modifier.padding(16.dp)) { content() }
    }
}

@Composable
fun AccentButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val p = LocalNexusPalette.current
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(13.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = p.accent,
            contentColor = p.background,
            disabledContainerColor = p.elevated,
            disabledContentColor = p.muted
        )
    ) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun QuietButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val p = LocalNexusPalette.current
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(13.dp),
        border = BorderStroke(1.dp, p.border),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = p.text)
    ) { Text(text) }
}

@Composable
fun SectionTitle(title: String, trailing: String? = null) {
    val p = LocalNexusPalette.current
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = p.text, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        if (trailing != null) Text(trailing, color = p.muted, fontSize = 11.sp)
    }
}

@Composable
fun MutedText(text: String, modifier: Modifier = Modifier, maxLines: Int = Int.MAX_VALUE) {
    Text(
        text = text,
        modifier = modifier,
        color = LocalNexusPalette.current.muted,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun NexusTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    placeholder: String = ""
) {
    val p = LocalNexusPalette.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { if (placeholder.isNotBlank()) Text(placeholder) },
        singleLine = singleLine,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = p.accent,
            unfocusedBorderColor = p.border,
            focusedTextColor = p.text,
            unfocusedTextColor = p.text,
            focusedLabelColor = p.accent,
            unfocusedLabelColor = p.muted,
            focusedContainerColor = p.surface,
            unfocusedContainerColor = p.surface,
            cursorColor = p.accent
        )
    )
}

@Composable
fun StatusPill(text: String, positive: Boolean = true) {
    val p = LocalNexusPalette.current
    val color = if (positive) p.accent else Color(0xFFFFC36E)
    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.12f)) {
        Text(text, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = color, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}
