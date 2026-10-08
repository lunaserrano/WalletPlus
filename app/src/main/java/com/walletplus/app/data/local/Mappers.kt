package com.walletplus.app.data.local

import com.walletplus.app.data.local.entities.AccountEntity
import com.walletplus.app.data.local.entities.BudgetEntity
import com.walletplus.app.data.local.entities.CategoryEntity
import com.walletplus.app.data.local.entities.SavingsGoalEntity
import com.walletplus.app.data.local.entities.TransactionEntity
import com.walletplus.app.data.local.entities.UserSettingsEntity
import com.walletplus.domain.model.Account
import com.walletplus.domain.model.Budget
import com.walletplus.domain.model.Category
import com.walletplus.domain.model.SavingsGoal
import com.walletplus.domain.model.SpendingPlan
import com.walletplus.domain.model.Transaction
import com.walletplus.domain.model.UserSettings
import com.walletplus.domain.money.Money

fun AccountEntity.toDomain() = Account(
    id = id,
    name = name,
    initialBalance = Money(initialBalanceCents),
    startDate = startDate,
    currencyCode = currencyCode,
    isArchived = isArchived,
)

fun Account.toEntity() = AccountEntity(
    id = id,
    name = name,
    initialBalanceCents = initialBalance.cents,
    startDate = startDate,
    currencyCode = currencyCode,
    isArchived = isArchived,
)

fun CategoryEntity.toDomain() = Category(
    id = id,
    name = name,
    emoji = emoji,
    colorKey = colorKey,
    type = type,
    isActive = isActive,
    systemKey = systemKey,
    sortOrder = sortOrder,
)

fun Category.toEntity() = CategoryEntity(
    id = id,
    name = name,
    emoji = emoji,
    colorKey = colorKey,
    type = type,
    isActive = isActive,
    systemKey = systemKey,
    sortOrder = sortOrder,
)

fun TransactionEntity.toDomain() = Transaction(
    id = id,
    accountId = accountId,
    type = type,
    amount = Money(amountCents),
    categoryId = categoryId,
    description = description,
    date = date,
    paymentMethod = paymentMethod,
    isNecessary = isNecessary,
    notes = notes,
    recurringId = recurringId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Transaction.toEntity() = TransactionEntity(
    id = id,
    accountId = accountId,
    type = type,
    amountCents = amount.cents,
    categoryId = categoryId,
    description = description,
    date = date,
    paymentMethod = paymentMethod,
    isNecessary = isNecessary,
    notes = notes,
    recurringId = recurringId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun BudgetEntity.toDomain() = Budget(id = id, categoryId = categoryId, limit = Money(limitCents), yearMonth = yearMonth)

fun Budget.toEntity() = BudgetEntity(id = id, categoryId = categoryId, limitCents = limit.cents, yearMonth = yearMonth)

fun SavingsGoalEntity.toDomain() = SavingsGoal(
    id = id,
    name = name,
    emoji = emoji,
    target = Money(targetCents),
    saved = Money(savedCents),
    targetDate = targetDate,
    createdAt = createdAt,
    isArchived = isArchived,
)

fun SavingsGoal.toEntity() = SavingsGoalEntity(
    id = id,
    name = name,
    emoji = emoji,
    targetCents = target.cents,
    savedCents = saved.cents,
    targetDate = targetDate,
    createdAt = createdAt,
    isArchived = isArchived,
)

fun UserSettingsEntity?.toDomain(): UserSettings {
    if (this == null) return UserSettings()
    return UserSettings(
        currencyCode = currencyCode,
        displayName = displayName,
        plan = SpendingPlan(
            expectedIncome = Money(expectedIncomeCents),
            spendingLimit = Money(spendingLimitCents),
            savingsTarget = Money(savingsTargetCents),
        ),
        onboardingCompleted = onboardingCompleted,
        demoMode = demoMode,
        lockEnabled = lockEnabled,
        tourCompleted = tourCompleted,
        payPeriod = payPeriod,
    )
}

/** Aplica los ajustes de dominio conservando las credenciales de seguridad existentes. */
fun UserSettings.toEntity(existing: UserSettingsEntity?) = UserSettingsEntity(
    currencyCode = currencyCode,
    displayName = displayName,
    expectedIncomeCents = plan.expectedIncome.cents,
    spendingLimitCents = plan.spendingLimit.cents,
    savingsTargetCents = plan.savingsTarget.cents,
    onboardingCompleted = onboardingCompleted,
    demoMode = demoMode,
    lockEnabled = lockEnabled,
    tourCompleted = tourCompleted,
    payPeriod = payPeriod,
    pinHash = existing?.pinHash,
    pinSalt = existing?.pinSalt,
)
