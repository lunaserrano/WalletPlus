package com.walletplus.domain.usecase

import com.walletplus.domain.model.Account
import com.walletplus.domain.model.Budget
import com.walletplus.domain.model.Category
import com.walletplus.domain.model.MoneyTotals
import com.walletplus.domain.model.SavingsGoal
import com.walletplus.domain.model.Transaction
import com.walletplus.domain.model.TransactionFilter
import com.walletplus.domain.model.TransactionType
import com.walletplus.domain.model.UserSettings
import com.walletplus.domain.money.sumOfMoney
import com.walletplus.domain.repository.AccountRepository
import com.walletplus.domain.repository.BudgetRepository
import com.walletplus.domain.repository.CategoryRepository
import com.walletplus.domain.repository.DataStoreMaintenance
import com.walletplus.domain.repository.GoalRepository
import com.walletplus.domain.repository.SettingsRepository
import com.walletplus.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class FakeAccounts(initial: Account? = null) : AccountRepository {
    val state = MutableStateFlow(initial)
    override fun observePrimary(): Flow<Account?> = state
    override suspend fun getPrimary(): Account? = state.value
    override suspend fun save(account: Account): Long {
        val saved = if (account.id == 0L) account.copy(id = 1) else account
        state.value = saved
        return saved.id
    }
}

class FakeCategories(initial: List<Category>) : CategoryRepository {
    val state = MutableStateFlow(initial)
    override fun observeAll(): Flow<List<Category>> = state
    override suspend fun getAll(): List<Category> = state.value
    override suspend fun getById(id: Long): Category? = state.value.firstOrNull { it.id == id }
    override suspend fun save(category: Category): Long {
        val id = if (category.id == 0L) (state.value.maxOfOrNull { it.id } ?: 0) + 1 else category.id
        state.value = state.value.filterNot { it.id == id } + category.copy(id = id)
        return id
    }
    override suspend fun setActive(id: Long, active: Boolean) {
        state.value = state.value.map { if (it.id == id) it.copy(isActive = active) else it }
    }
    override suspend fun delete(id: Long) {
        state.value = state.value.filterNot { it.id == id }
    }
}

class FakeTransactions : TransactionRepository {
    val state = MutableStateFlow<List<Transaction>>(emptyList())
    override fun observeTotals(accountId: Long): Flow<MoneyTotals> = state.map { list ->
        val own = list.filter { it.accountId == accountId }
        MoneyTotals(own.filter { it.isIncome }.sumOfMoney { it.amount }, own.filter { it.isExpense }.sumOfMoney { it.amount })
    }
    override fun observeBetween(from: LocalDate, to: LocalDate): Flow<List<Transaction>> =
        state.map { list -> list.filter { !it.date.isBefore(from) && !it.date.isAfter(to) } }
    override fun observeFiltered(filter: TransactionFilter): Flow<List<Transaction>> = state
    override suspend fun getById(id: Long): Transaction? = state.value.firstOrNull { it.id == id }
    override suspend fun save(transaction: Transaction): Long {
        val id = if (transaction.id == 0L) (state.value.maxOfOrNull { it.id } ?: 0) + 1 else transaction.id
        state.value = state.value.filterNot { it.id == id } + transaction.copy(id = id)
        return id
    }
    override suspend fun saveAll(transactions: List<Transaction>) = transactions.forEach { save(it) }
    override suspend fun delete(id: Long) {
        state.value = state.value.filterNot { it.id == id }
    }
    override suspend fun countByCategory(categoryId: Long): Int = state.value.count { it.categoryId == categoryId }
    override suspend fun lastUsedCategoryId(type: TransactionType): Long? =
        state.value.filter { it.type == type }.maxByOrNull { it.createdAt }?.categoryId
}

class FakeBudgets : BudgetRepository {
    val state = MutableStateFlow<List<Budget>>(emptyList())
    override fun observeAll(): Flow<List<Budget>> = state
    override suspend fun getByCategory(categoryId: Long): Budget? = state.value.firstOrNull { it.categoryId == categoryId }
    override suspend fun save(budget: Budget): Long {
        val id = if (budget.id == 0L) (state.value.maxOfOrNull { it.id } ?: 0) + 1 else budget.id
        state.value = state.value.filterNot { it.id == id } + budget.copy(id = id)
        return id
    }
    override suspend fun delete(id: Long) {
        state.value = state.value.filterNot { it.id == id }
    }
}

class FakeGoals : GoalRepository {
    val state = MutableStateFlow<List<SavingsGoal>>(emptyList())
    override fun observeAll(): Flow<List<SavingsGoal>> = state
    override suspend fun getById(id: Long): SavingsGoal? = state.value.firstOrNull { it.id == id }
    override suspend fun save(goal: SavingsGoal): Long {
        val id = if (goal.id == 0L) (state.value.maxOfOrNull { it.id } ?: 0) + 1 else goal.id
        state.value = state.value.filterNot { it.id == id } + goal.copy(id = id)
        return id
    }
    override suspend fun delete(id: Long) {
        state.value = state.value.filterNot { it.id == id }
    }
}

class FakeSettings : SettingsRepository {
    val state = MutableStateFlow(UserSettings())
    override fun observe(): Flow<UserSettings> = state
    override suspend fun get(): UserSettings = state.value
    override suspend fun update(transform: (UserSettings) -> UserSettings) {
        state.value = transform(state.value)
    }
}

class FakeMaintenance(
    private val transactions: FakeTransactions,
    private val budgets: FakeBudgets,
    private val goals: FakeGoals,
    private val accounts: FakeAccounts,
) : DataStoreMaintenance {
    override suspend fun <R> atomically(block: suspend () -> R): R = block()
    override suspend fun clearUserData() {
        transactions.state.value = emptyList()
        budgets.state.value = emptyList()
        goals.state.value = emptyList()
        accounts.state.value = null
    }
}
