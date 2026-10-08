package com.walletplus.domain.model

/** Categorías iniciales. La clave es estable para poder referenciarlas (p. ej. datos demo). */
object DefaultCategories {

    data class Seed(
        val key: String,
        val name: String,
        val emoji: String,
        val colorKey: String,
        val type: TransactionType,
    )

    val expense = listOf(
        Seed("food", "Alimentación", "🛒", "aqua", TransactionType.EXPENSE),
        Seed("transport", "Transporte", "🚌", "blue", TransactionType.EXPENSE),
        Seed("housing", "Vivienda", "🏠", "violet", TransactionType.EXPENSE),
        Seed("utilities", "Servicios", "💡", "yellow", TransactionType.EXPENSE),
        Seed("health", "Salud", "🩺", "red", TransactionType.EXPENSE),
        Seed("education", "Educación", "📚", "green", TransactionType.EXPENSE),
        Seed("entertainment", "Entretenimiento", "🎬", "magenta", TransactionType.EXPENSE),
        Seed("shopping", "Compras", "🛍️", "orange", TransactionType.EXPENSE),
        Seed("debt", "Deudas", "💳", "red", TransactionType.EXPENSE),
        Seed("subscriptions", "Suscripciones", "🔁", "blue", TransactionType.EXPENSE),
        Seed("other_expense", "Otros", "📦", "slate", TransactionType.EXPENSE),
    )

    val income = listOf(
        Seed("salary", "Salario", "💼", "aqua", TransactionType.INCOME),
        Seed("freelance", "Trabajo independiente", "🧑‍💻", "blue", TransactionType.INCOME),
        Seed("sales", "Ventas", "🏷️", "yellow", TransactionType.INCOME),
        Seed("commission", "Comisiones", "📈", "violet", TransactionType.INCOME),
        Seed("gift", "Regalos", "🎁", "magenta", TransactionType.INCOME),
        Seed("other_income", "Otros ingresos", "✨", "slate", TransactionType.INCOME),
    )

    val all = expense + income

    fun asCategories(): List<Category> = all.mapIndexed { index, seed ->
        Category(
            name = seed.name,
            emoji = seed.emoji,
            colorKey = seed.colorKey,
            type = seed.type,
            systemKey = seed.key,
            sortOrder = index,
        )
    }
}
