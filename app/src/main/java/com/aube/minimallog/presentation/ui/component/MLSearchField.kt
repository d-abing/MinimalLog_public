package com.aube.minimallog.presentation.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.aube.minimallog.R

@Composable
fun RowScope.MLSearchField(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier.weight(1f)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {onSearchQueryChanged(it)},
            placeholder = {
                Text(
                    stringResource(R.string.placeholder_memory_search),
                    style = MaterialTheme.typography.labelLarge
                )
            },
            textStyle = MaterialTheme.typography.labelLarge,
            modifier = Modifier
                .fillMaxWidth(),
            singleLine = true,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                )
            },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboardController ?.hide()
                    }
                ),
            colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
            )
        )

        HorizontalDivider(
            modifier = Modifier
                    .padding(start = 16.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        )
    }
}