package com.dagothig.cafaitlajobremote

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat

class SettingsFragment: PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        for (key in arrayOf("pref_cursor_h_mult", "pref_cursor_v_mult", "pref_wheel_mul")) {
            val pref = preferenceManager.findPreference<EditTextPreference>(key)
            pref?.setOnBindEditTextListener { editText ->
                editText.inputType = InputType.TYPE_CLASS_NUMBER
                    .or(InputType.TYPE_NUMBER_FLAG_DECIMAL)
                    .or(InputType.TYPE_NUMBER_FLAG_SIGNED)
            }
        }
    }
}

class PreferencesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preferences)

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, SettingsFragment())
            .commit()
    }
}