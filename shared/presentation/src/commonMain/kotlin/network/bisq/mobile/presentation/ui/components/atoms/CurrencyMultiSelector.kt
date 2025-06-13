package network.bisq.mobile.presentation.ui.components.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.flow.MutableStateFlow
import network.bisq.mobile.domain.fiatNameByCode
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.atoms.icons.ArrowDownIcon
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@Composable
fun BisqCurrencyMultiSelector(
    selectedCurrencies: Set<String>,
    onSelectionChanged: (Set<String>) -> Unit,
    label: String = "Currencies",
    placeholder: String = "Select currencies",
    modifier: Modifier = Modifier,
    isRequired: Boolean = false
) {
    var showDialog by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    
    // Display text for selected currencies
    val displayText = when {
        selectedCurrencies.isEmpty() && !isRequired -> "Any Currency"
        selectedCurrencies.isEmpty() -> placeholder
        selectedCurrencies.size == 1 -> {
            val code = selectedCurrencies.first()
            "${fiatNameByCode[code] ?: code} ($code)"
        }
        selectedCurrencies.size <= 3 -> {
            selectedCurrencies.joinToString(", ") { code ->
                fiatNameByCode[code] ?: code
            }
        }
        else -> "${selectedCurrencies.size} currencies selected"
    }

    Column(modifier = modifier) {
        if (label.isNotEmpty()) {
            BisqText.smallRegular(
                text = label,
                color = BisqTheme.colors.mid_grey20
            )
            BisqGap.VQuarter()
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(BisqTheme.colors.dark_grey40)
                .clickable(
                    onClick = { showDialog = true },
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BisqText.baseRegular(
                    text = displayText,
                    color = if (selectedCurrencies.isEmpty() && isRequired)
                        BisqTheme.colors.mid_grey20
                    else
                        BisqTheme.colors.white,
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                ArrowDownIcon()
            }
        }
    }

    if (showDialog) {
        CurrencySelectionDialog(
            selectedCurrencies = selectedCurrencies,
            onSelectionChanged = onSelectionChanged,
            onDismiss = { showDialog = false },
            searchText = searchText,
            onSearchTextChanged = { searchText = it },
            isRequired = isRequired
        )
    }
}

@Composable
private fun CurrencySelectionDialog(
    selectedCurrencies: Set<String>,
    onSelectionChanged: (Set<String>) -> Unit,
    onDismiss: () -> Unit,
    searchText: String,
    onSearchTextChanged: (String) -> Unit,
    isRequired: Boolean
) {
    val filteredCurrencies = remember(searchText) {
        fiatNameByCode.filter { (code, name) ->
            if (searchText.isBlank()) true
            else {
                code.contains(searchText, ignoreCase = true) ||
                name.contains(searchText, ignoreCase = true)
            }
        }.toList().sortedBy { it.second }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(12.dp),
            color = BisqTheme.colors.backgroundColor
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BisqText.h5Regular("Select Currencies")
                    
                    if (!isRequired) {
                        BisqButton(
                            text = "Clear All",
                            type = BisqButtonType.Grey,
                            onClick = { 
                                onSelectionChanged(emptySet())
                                onDismiss()
                            },
                            padding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                BisqGap.V1()

                // Search field
                BisqTextField(
                    value = searchText,
                    onValueChange = { text, _ -> onSearchTextChanged(text) },
                    placeholder = "Search currencies...",
                    modifier = Modifier.fillMaxWidth()
                )

                BisqGap.V1()

                // Selected count
                if (selectedCurrencies.isNotEmpty()) {
                    BisqText.smallRegular(
                        "${selectedCurrencies.size} currencies selected",
                        color = BisqTheme.colors.primary
                    )
                    BisqGap.VHalf()
                }

                // Currency list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredCurrencies) { (code, name) ->
                        CurrencyItem(
                            code = code,
                            name = name,
                            isSelected = selectedCurrencies.contains(code),
                            onToggle = { 
                                val newSelection = if (selectedCurrencies.contains(code)) {
                                    selectedCurrencies - code
                                } else {
                                    selectedCurrencies + code
                                }
                                onSelectionChanged(newSelection)
                            }
                        )
                    }
                }

                BisqGap.V1()

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BisqButton(
                        text = "Cancel",
                        type = BisqButtonType.Grey,
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )
                    
                    BisqGap.H1()
                    
                    BisqButton(
                        text = "Done",
                        onClick = onDismiss,
                        disabled = isRequired && selectedCurrencies.isEmpty(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrencyItem(
    code: String,
    name: String,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (isSelected) BisqTheme.colors.primaryDim 
                else BisqTheme.colors.dark_grey50
            )
            .clickable(
                onClick = onToggle,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = BisqTheme.colors.primary,
                uncheckedColor = BisqTheme.colors.mid_grey20,
                checkmarkColor = BisqTheme.colors.dark_grey10
            )
        )
        
        BisqGap.H1()
        
        Column(modifier = Modifier.weight(1f)) {
            BisqText.baseRegular(name)
            BisqText.smallRegular(code, color = BisqTheme.colors.mid_grey20)
        }
    }
}
