package com.eficazautomotriz.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.eficazautomotriz.app.ui.theme.EficazColors
import com.eficazautomotriz.app.ui.theme.EficazDimens
import com.eficazautomotriz.app.ui.theme.SectionLabelStyle

private val FieldShape = RoundedCornerShape(EficazDimens.FieldRadius)

/**
 * Form field of the mockups: white rounded box with the uppercase label INSIDE the box
 * ("CORREO ELECTRÓNICO") and the value below it. Red border when [isError]; the error text
 * itself is shown by the caller (see [FieldError]) so several fields can share one line.
 */
@Composable
fun BoxedTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    singleLine: Boolean = true,
    maxLength: Int? = null,
    inputFilter: InputFilter = InputFilters.None,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    showCounter: Boolean = false,
) {
    var focused by remember { mutableStateOf(false) }
    val borderColor = when {
        isError -> EficazColors.DangerText
        focused -> EficazColors.Navy
        else -> EficazColors.FieldBorder
    }
    Column(
        modifier
            .revealWhenFocused()
            .fillMaxWidth()
            .clip(FieldShape)
            .background(EficazColors.Surface)
            .border(if (focused || isError) 1.5.dp else 1.dp, borderColor, FieldShape)
            .padding(horizontal = EficazDimens.FieldPaddingHorizontal, vertical = EficazDimens.FieldPaddingVertical),
    ) {
        Text(label.uppercase(), style = SectionLabelStyle, color = EficazColors.Navy)
        Spacer(Modifier.height(EficazDimens.SpaceXxs))
        BasicTextField(
            value = value,
            onValueChange = { typed -> onValueChange(InputFilters.apply(typed, inputFilter, maxLength)) },
            enabled = enabled,
            singleLine = singleLine,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = EficazColors.TextPrimary),
            cursorBrush = SolidColor(EficazColors.Navy),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            visualTransformation = if (isPassword) PasswordVisualTransformation('●') else visualTransformation,
            modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
            decorationBox = { inner ->
                if (value.isEmpty() && placeholder != null) {
                    Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = EficazColors.TextMuted)
                }
                inner()
            },
        )
        if (showCounter && maxLength != null) {
            Text(
                "${value.length}/$maxLength",
                style = MaterialTheme.typography.bodySmall,
                color = EficazColors.TextMuted,
                modifier = Modifier.align(Alignment.End),
            )
        }
    }
}

/** Inline red error line with a ⚠ prefix, as under the login fields in f1. */
@Composable
fun FieldError(text: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth()) {
        Text("⚠", color = EficazColors.DangerText, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.width(EficazDimens.SpaceXs))
        Text(text, color = EficazColors.DangerText, style = MaterialTheme.typography.bodyMedium)
    }
}
