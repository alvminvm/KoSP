package me.alzz.kosp

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import android.content.SharedPreferences
import me.alzz.kosp.ObservablePreference.Companion.preferenceMap
import kotlin.jvm.internal.CallableReference
import kotlin.reflect.KProperty

/**
 * 可观察变化的 pref 属性
 * Created by JeremyHe on 2019-07-18.
 */
class ObservablePreference<T>(
        prefFileName: String,
        sp: SharedPreferences,
        name: String,
        default: T,
        encrypt: Boolean = false) : Preference<T>(prefFileName, sp, name, default, encrypt) {

    internal val notify by lazy {
        val liveData = MutableLiveData<T>()
        liveData.postValue(getValue(null, property))
        liveData
    }

    operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ObservablePreference<T> {
        property = prop
        if (default is ObservablePreference<*>) {
            default.property = property
        } else {
            val key = "$prefFileName#${prop.name}"
            preferenceMap[key] = this
        }

        return this
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        super.setValue(thisRef, property, value)
        notify.postValue(value)
    }

    companion object {
        internal val preferenceMap = mutableMapOf<String, ObservablePreference<*>>()
    }
}

fun <T> KProperty<T>.toLiveData(): MutableLiveData<T>? {
    val prefFileName = ((this as? CallableReference)?.boundReceiver as? KoSharePrefs)?.prefName ?: return null
    val key = "$prefFileName#$name"
    return preferenceMap[key]?.notify as? MutableLiveData<T> ?: return null
}

fun <T> KProperty<T>.observe(owner: LifecycleOwner, block: (T)->Unit) {
    val data = toLiveData() ?: return
    data.observe(owner, Observer {
        it ?: return@Observer
        block(it)
    })
}