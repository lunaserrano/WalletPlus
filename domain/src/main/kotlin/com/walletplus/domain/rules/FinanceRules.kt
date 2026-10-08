package com.walletplus.domain.rules

import com.walletplus.domain.money.Money
import com.walletplus.domain.money.Percent

/**
 * Umbrales centralizados. Cambiar un valor aquí ajusta todo el comportamiento de la app.
 */
data class BudgetThresholds(
    /** Desde este porcentaje el presupuesto pasa a "Precaución". */
    val caution: Percent = Percent.ofWhole(70),
    /** Desde este porcentaje el presupuesto pasa a "Alto". */
    val high: Percent = Percent.ofWhole(90),
    /** Por encima (estricto) de este porcentaje el presupuesto está "Excedido". */
    val exceeded: Percent = Percent.ofWhole(100),
)

data class InsightThresholds(
    /** Regla 1: proporción de gastos opcionales que merece atención. */
    val optionalShareAlert: Percent = Percent.ofWhole(20),
    /** Regla 2: aumento relativo de una categoría respecto al mes anterior. */
    val categoryIncreaseAlert: Percent = Percent.ofWhole(20),
    /** Gasto mínimo del mes anterior para que una comparación sea significativa. */
    val minimumComparableAmount: Money = Money.ofUnits(10),
    /** Gastos por debajo de este monto se consideran "pequeños". */
    val smallExpenseLimit: Money = Money.ofUnits(5),
    /** Cantidad mínima de gastos pequeños para considerarlos frecuentes. */
    val smallExpenseMinCount: Int = 4,
    /** Suma mínima de gastos pequeños para que sea relevante mencionarla. */
    val smallExpenseMinTotal: Money = Money.ofUnits(15),
)

data class FinanceRules(
    val budget: BudgetThresholds = BudgetThresholds(),
    val insights: InsightThresholds = InsightThresholds(),
) {
    companion object {
        val Default = FinanceRules()
    }
}
