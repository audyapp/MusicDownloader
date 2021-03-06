package com.andreacioccarelli.musicdownloader.extensions

import android.content.Context
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodManager.SHOW_FORCED
import android.widget.EditText
import android.widget.TextView
import com.andreacioccarelli.musicdownloader.App
import com.andreacioccarelli.musicdownloader.R

/**
 *  Designed and developed by Andrea Cioccarelli
 */

fun EditText.dismissKeyboard() {
    val imm = App.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(this.windowToken, 0)
}

fun EditText.popUpKeyboard() {
    val imm = App.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.showSoftInput(this, SHOW_FORCED)
}

fun EditText.onSubmit(code: () -> Unit) {
    setOnEditorActionListener { _, actionId, _ ->
        when (actionId) {
            EditorInfo.IME_ACTION_DONE,
            EditorInfo.IME_ACTION_NONE,
            EditorInfo.IME_ACTION_GO,
            EditorInfo.IME_ACTION_SEARCH,
            EditorInfo.IME_ACTION_SEND,
            EditorInfo.IME_ACTION_NEXT -> code()
        }
        true
    }
}

fun TextView.applyChecklistBadge(applyBadge: Boolean) {
    if (applyBadge) {
        setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.badge_checklisted, 0)
    } else {
        setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
    }
}