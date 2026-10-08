package com.walletplus.domain

import com.walletplus.domain.model.Category
import com.walletplus.domain.model.PaymentMethod
import com.walletplus.domain.model.Transaction
import com.walletplus.domain.model.TransactionType
import com.walletplus.domain.money.Money
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

val TODAY: LocalDate = LocalDate.of(2026, 10, 15)
val FIXED_CLOCK: Clock = Clock.fixed(TODAY.atTime(10, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

val FOOD = Category(id = 1, name = "Alimentación", emoji = "🛒", colorKey = "green", type = TransactionType.EXPENSE)
val TRANSPORT = Category(id = 2, name = "Transporte", emoji = "🚌", colorKey = "blue", type = TransactionType.EXPENSE)
val FUN = Category(id = 3, name = "Entretenimiento", emoji = "🎬", colorKey = "pink", type = TransactionType.EXPENSE)
val SALARY = Category(id = 10, name = "Salario", emoji = "💼", colorKey = "green", type = TransactionType.INCOME)
val CATEGORIES = listOf(FOOD, TRANSPORT, FUN, SALARY)

fun usd(units: Long, cents: Int = 0) = Money.ofUnits(units, cents)

private var nextId = 1L

fun expense(
    amount: Money,
    category: Category = FOOD,
    date: LocalDate = TODAY,
    necessary: Boolean = true,
) = Transaction(
    id = nextId++,
    accountId = 1,
    type = TransactionType.EXPENSE,
    amount = amount,
    categoryId = category.id,
    description = "",
    date = date,
    paymentMethod = PaymentMethod.CASH,
    isNecessary = necessary,
    createdAt = Instant.EPOCH,
    updatedAt = Instant.EPOCH,
)

fun income(amount: Money, date: LocalDate = TODAY, category: Category = SALARY) = Transaction(
    id = nextId++,
    accountId = 1,
    type = TransactionType.INCOME,
    amount = amount,
    categoryId = category.id,
    description = "",
    date = date,
    paymentMethod = PaymentMethod.TRANSFER,
    isNecessary = null,
    createdAt = Instant.EPOCH,
    updatedAt = Instant.EPOCH,
)
