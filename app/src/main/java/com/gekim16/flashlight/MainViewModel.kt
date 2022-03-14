package com.gekim16.flashlight

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel: ViewModel() {
    val state = MutableLiveData<Boolean>()

    fun setState() {
        if(state.value == null) {
            state.value = false
        }
        state.value = state.value != true
    }
}