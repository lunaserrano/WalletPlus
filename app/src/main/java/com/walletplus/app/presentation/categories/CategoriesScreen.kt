package com.walletplus.app.presentation.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.walletplus.app.core.di.walletViewModel
import com.walletplus.app.core.ui.components.CategoryAvatar
import com.walletplus.app.core.ui.components.LocalMessenger
import com.walletplus.app.core.ui.components.Pill
import com.walletplus.app.core.ui.components.PrimaryButton
import com.walletplus.app.core.ui.components.SegmentedControl
import com.walletplus.app.core.ui.components.WalletCard
import com.walletplus.app.core.ui.components.WalletSheet
import com.walletplus.app.core.ui.components.categoryColor
import com.walletplus.app.core.ui.components.walletFieldColors
import com.walletplus.app.core.ui.theme.CategoryPalette
import com.walletplus.app.core.ui.theme.Spacing
import com.walletplus.app.presentation.common.message
import com.walletplus.domain.model.Category
import com.walletplus.domain.model.TransactionType
import com.walletplus.domain.repository.CategoryRepository
import com.walletplus.domain.usecase.RemoveCategoryUseCase
import com.walletplus.domain.usecase.SaveCategoryUseCase
import com.walletplus.domain.usecase.SetCategoryActiveUseCase
import com.walletplus.domain.validation.Outcome
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CategoriesViewModel(
    repository: CategoryRepository,
    private val save: SaveCategoryUseCase,
    private val setActive: SetCategoryActiveUseCase,
    private val remove: RemoveCategoryUseCase,
) : ViewModel() {
    val categories = repository.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    suspend fun save(id: Long, name: String, emoji: String, color: String, type: TransactionType) = save.invoke(id, name, emoji, color, type)
    fun toggle(category: Category) = viewModelScope.launch { setActive(category.id, !category.isActive) }
    suspend fun remove(id: Long) = remove.invoke(id)
}

private val EmojiOptions = listOf(
    "🛒", "🍔", "☕", "🍕", "🚌", "🚗", "⛽", "🏠", "💡", "💧", "📱", "🌐", "🩺", "💊", "📚", "🎓",
    "🎬", "🎮", "🎵", "🛍️", "👕", "💳", "🔁", "🎁", "🐶", "✈️", "🏋️", "💇", "🧾", "📦", "💼", "🧑‍💻",
    "🏷️", "📈", "✨", "💰",
)

@Composable
fun CategoriesScreen(onBack: () -> Unit) {
    val viewModel = walletViewModel { CategoriesViewModel(it.categories, it.saveCategory, it.setCategoryActive, it.removeCategory) }
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<Category?>(null) }
    var creating by remember { mutableStateOf(false) }
    val type = if (tab == 0) TransactionType.EXPENSE else TransactionType.INCOME
    val visible = categories.filter { it.type == type }.sortedWith(compareByDescending<Category> { it.isActive }.thenBy { it.sortOrder })

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = Spacing.screen, end = Spacing.screen, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            item {
                Row(Modifier.padding(top = Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver") }
                    Text("Categorías", style = MaterialTheme.typography.headlineSmall)
                }
            }
            item {
                SegmentedControl(listOf("Gastos", "Ingresos"), tab, { tab = it }, modifier = Modifier.padding(vertical = Spacing.sm))
            }
            item {
                Text(
                    "Las categorías con movimientos no se borran: se desactivan y siguen visibles en tu historial.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Spacing.sm),
                )
            }
            items(visible, key = { it.id }) { category ->
                WalletCard(
                    onClick = { editing = category },
                    contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.md),
                    modifier = Modifier.animateItem(),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CategoryAvatar(category.emoji, category.colorKey, muted = !category.isActive)
                        Spacer(Modifier.size(Spacing.md))
                        Column(Modifier.weight(1f)) {
                            Text(
                                category.name,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (category.isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (!category.isActive) {
                                Spacer(Modifier.height(2.dp))
                                Pill("Desactivada", MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(checked = category.isActive, onCheckedChange = { viewModel.toggle(category) })
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { creating = true },
            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
            text = { Text("Nueva categoría") },
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(Spacing.xl),
        )
    }

    if (creating || editing != null) {
        CategorySheet(
            category = editing,
            type = editing?.type ?: type,
            viewModel = viewModel,
            onDismiss = {
                creating = false
                editing = null
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategorySheet(category: Category?, type: TransactionType, viewModel: CategoriesViewModel, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val messenger = LocalMessenger.current
    var name by remember { mutableStateOf(category?.name.orEmpty()) }
    var emoji by remember { mutableStateOf(category?.emoji ?: EmojiOptions.first()) }
    var color by remember { mutableStateOf(category?.colorKey ?: CategoryPalette.keys.first()) }
    var error by remember { mutableStateOf<String?>(null) }

    WalletSheet(
        title = if (category == null) "Nueva categoría" else "Editar categoría",
        onDismiss = onDismiss,
        trailing = category?.let {
            {
                IconButton(onClick = {
                    scope.launch {
                        val result = viewModel.remove(it.id)
                        messenger.show(
                            when (result) {
                                RemoveCategoryUseCase.Result.DELETED -> "Categoría eliminada"
                                RemoveCategoryUseCase.Result.DEACTIVATED -> "Tiene movimientos: se desactivó para conservar tu historial"
                                RemoveCategoryUseCase.Result.NOT_FOUND -> "La categoría ya no existe"
                            },
                        )
                        onDismiss()
                    }
                }) { Icon(Icons.Rounded.DeleteOutline, contentDescription = "Eliminar categoría", tint = MaterialTheme.colorScheme.error) }
            }
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            CategoryAvatar(emoji, color, size = 56.dp)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(SaveCategoryUseCase.MAX_NAME_LENGTH); error = null },
                label = { Text("Nombre") },
                singleLine = true,
                isError = error != null,
                supportingText = error?.let { { Text(it) } },
                shape = MaterialTheme.shapes.medium,
                colors = walletFieldColors(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                modifier = Modifier.weight(1f),
            )
        }
        Text("Ícono", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            EmojiOptions.forEach { option ->
                val selected = option == emoji
                Surface(
                    onClick = { emoji = option },
                    shape = MaterialTheme.shapes.small,
                    color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) { Text(option, style = MaterialTheme.typography.titleMedium) }
                }
            }
        }
        Text("Color", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            CategoryPalette.keys.forEach { key ->
                val selected = key == color
                val swatch = categoryColor(key)
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(swatch)
                        .border(if (selected) 3.dp else 0.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = if (selected) 0.85f else 0f), CircleShape)
                        .clickable { color = key },
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected) Icon(Icons.Rounded.Check, contentDescription = "Seleccionado", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
        PrimaryButton(if (category == null) "Crear categoría" else "Guardar cambios", onClick = {
            scope.launch {
                when (val result = viewModel.save(category?.id ?: 0, name, emoji, color, type)) {
                    is Outcome.Success -> {
                        messenger.show(if (category == null) "Categoría creada" else "Categoría actualizada")
                        onDismiss()
                    }
                    is Outcome.Invalid -> error = result.errors.first().message()
                }
            }
        })
        Spacer(Modifier.fillMaxWidth().height(0.dp))
    }
}
