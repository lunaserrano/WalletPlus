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
        Seed("food", "Alimentación", "🛒", "green", TransactionType.EXPENSE),
        Seed("transport", "Transporte", "🚌", "blue", TransactionType.EXPENSE),
        Seed("housing", "Vivienda", "🏠", "indigo", TransactionType.EXPENSE),
        Seed("utilities", "Servicios", "💡", "amber", TransactionType.EXPENSE),
        Seed("health", "Salud", "🩺", "red", TransactionType.EXPENSE),
        Seed("education", "Educación", "📚", "cyan", TransactionType.EXPENSE),
        Seed("entertainment", "Entretenimiento", "🎬", "pink", TransactionType.EXPENSE),
        Seed("shopping", "Compras", "🛍️", "purple", TransactionType.EXPENSE),
        Seed("debt", "Deudas", "💳", "orange", TransactionType.EXPENSE),
        Seed("subscriptions", "Suscripciones", "🔁", "teal", TransactionType.EXPENSE),
        Seed("other_expense", "Otros", "📦", "slate", TransactionType.EXPENSE),
    )

    val income = listOf(
        Seed("salary", "Salario", "💼", "green", TransactionType.INCOME),
        Seed("freelance", "Trabajo independiente", "🧑‍💻", "blue", TransactionType.INCOME),
        Seed("sales", "Ventas", "🏷️", "amber", TransactionType.INCOME),
        Seed("commission", "Comisiones", "📈", "teal", TransactionType.INCOME),
        Seed("gift", "Regalos", "🎁", "pink", TransactionType.INCOME),
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
