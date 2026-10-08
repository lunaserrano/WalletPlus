package com.walletplus.app.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.walletplus.app.core.format.DateFormats
import com.walletplus.app.core.format.LocalMoneyFormatter
import com.walletplus.app.core.ui.components.CategoryAvatar
import com.walletplus.app.core.ui.components.ClickableRow
import com.walletplus.app.core.ui.components.IconBadge
import com.walletplus.app.core.ui.components.MoneyText
import com.walletplus.app.core.ui.components.WalletCard
import com.walletplus.app.core.ui.theme.Spacing
import com.walletplus.app.core.ui.theme.WalletTheme
import com.walletplus.domain.insights.Insight
import com.walletplus.domain.model.Category
import com.walletplus.domain.model.Transaction
import java.time.LocalDate

/** Fila de movimiento usada en el Dashboard y el historial. */
@Composable
fun TransactionRow(
    transaction: Transaction,
    category: Category?,
    today: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDate: Boolean = true,
) {
    ClickableRow(onClick = onClick, modifier = modifier) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            CategoryAvatar(category?.emoji ?: "•", category?.colorKey ?: "slate")
            Column(Modifier.weight(1f)) {
                Text(
                    transaction.description.ifBlank { category?.name ?: "Movimiento" },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val details = buildList {
                    if (transaction.description.isNotBlank()) category?.name?.let(::add)
                    if (showDate) add(DateFormats.relativeDay(transaction.date, today))
                    add(transaction.paymentMethod.shortLabel())
                    if (transaction.isOptionalExpense) add("Opcional")
                }
                Text(
                    details.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val amount = if (transaction.isIncome) transaction.amount else -transaction.amount
            MoneyText(
                amount = amount,
                signed = true,
                style = MaterialTheme.typography.titleSmall,
                color = if (transaction.isIncome) WalletTheme.colors.income else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
fun InsightCard(
    insight: Insight,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val copy = insight.copy(LocalMoneyFormatter.current)
    WalletCard(modifier = modifier, onClick = onClick) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            IconBadge(insight.icon(), insight.tone.color())
            Column(Modifier.weight(1f)) {
                Text(copy.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(copy.body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
