package ir.hamedan.budgetmanagement.ui.screens.draft

import ir.hamedan.budgetmanagement.ui.screens.add.AddTransactionFormState
import ir.hamedan.budgetmanagement.ui.screens.categories.CategoryFormState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DraftCodecsTest {

    @Test
    fun emptyForms_produceNoDraft() {
        assertNull(DraftCodecs.transaction(AddTransactionFormState(isExpense = true), "IRT"))
        assertNull(DraftCodecs.category(CategoryFormState()))
    }

    @Test
    fun transaction_roundTrips() {
        val form = AddTransactionFormState(
            isExpense = false, amount = "250000", categoryKey = "SALARY",
            title = "پاداش", note = "ماه آخر", step = 2, dayOffset = 1
        )
        val json = DraftCodecs.transaction(form, "IRT")
        assertNotNull(json)

        val restored = AddTransactionFormState(isExpense = true)
        DraftCodecs.applyTransaction(restored, json!!, "IRT")
        assertEquals(false, restored.isExpense)
        assertEquals("250000", restored.amount)
        assertEquals("SALARY", restored.categoryKey)
        assertEquals("پاداش", restored.title)
        assertEquals("ماه آخر", restored.note)
        assertEquals(2, restored.step)
        assertEquals(1, restored.dayOffset)
    }

    @Test
    fun transaction_amountFollowsCurrentUnit() {
        val form = AddTransactionFormState(isExpense = true, amount = "1000", title = "نان")
        val json = DraftCodecs.transaction(form, "IRT")!!
        val restored = AddTransactionFormState(isExpense = true)
        DraftCodecs.applyTransaction(restored, json, "IRR")
        assertEquals("10000", restored.amount)
    }

    @Test
    fun transaction_stepFallsBackWhenPrerequisitesMissing() {
        val form = AddTransactionFormState(isExpense = true, title = "فقط عنوان", step = 2)
        val restored = AddTransactionFormState(isExpense = true)
        DraftCodecs.applyTransaction(restored, DraftCodecs.transaction(form, "IRT")!!, "IRT")
        assertEquals(0, restored.step) // مبلغ ندارد
    }

    @Test
    fun category_roundTripsAndClampsStep() {
        val form = CategoryFormState(title = "ورزش", emoji = "", step = 2)
        val restored = CategoryFormState()
        DraftCodecs.applyCategory(restored, DraftCodecs.category(form)!!)
        assertEquals("ورزش", restored.title)
        assertEquals(1, restored.step) // آیکون ندارد
    }
}