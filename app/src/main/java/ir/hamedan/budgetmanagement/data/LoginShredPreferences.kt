package ir.hamedan.budgetmanagement.data

import android.content.Context

object SharedPreferences {
    private fun getSharedPreferences(context: Context)=
        context.getSharedPreferences("Test",Context.MODE_PRIVATE) // تعریف نام و سطح دسترسی

    fun setIsLog(context: Context,isLog : Boolean)=
        getSharedPreferences(context).edit().putBoolean("is_login",isLog)

    fun getIsLog(context: Context)=
        getSharedPreferences(context).getBoolean("is_login",false)
}