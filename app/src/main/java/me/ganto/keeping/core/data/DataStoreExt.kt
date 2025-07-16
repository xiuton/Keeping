package me.ganto.keeping.core.data

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore by preferencesDataStore(name = "bills")
val PREF_KEY_EXP_CAT = stringPreferencesKey("expense_categories")
val PREF_KEY_INC_CAT = stringPreferencesKey("income_categories")
val PREF_KEY_EXP_PAY = stringPreferencesKey("expense_pay_types")
val PREF_KEY_INC_PAY = stringPreferencesKey("income_pay_types")
// 可根据需要添加更多 key 