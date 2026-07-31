package ir.hamedan.budgetmanagement

import android.app.Application
import ir.hamedan.budgetmanagement.di.AppContainer

class BudgetApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}