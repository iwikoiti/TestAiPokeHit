package com.example.testaipokehit.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.testaipokehit.R
import com.example.testaipokehit.model.PokemonType
import com.example.testaipokehit.screens.common.TypeChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSheet(
    selected: Set<PokemonType>,
    onApply: (Set<PokemonType>) -> Unit,
    onDismiss: () -> Unit
) {
    var localSelection by remember { mutableStateOf(selected) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = stringResource(R.string.filter_title),
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(8.dp))
            PokemonType.entries.forEach { type ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            localSelection =
                                if (type in localSelection) localSelection - type else localSelection + type
                        }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = type in localSelection,
                        onCheckedChange = { checked ->
                            localSelection =
                                if (checked) localSelection + type else localSelection - type
                        }
                    )
                    TypeChip(type)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = { localSelection = emptySet() }) {
                    Text(stringResource(R.string.reset_all))
                }
                Button(
                    onClick = {
                        onApply(localSelection)
                        onDismiss()
                    }
                ) {
                    Text(stringResource(R.string.apply))
                }
            }
        }
    }
}