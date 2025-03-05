package dev.brahmkshatriya.echo.utils.ui

import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class AutoClearedValue<T : Any>(val fragment: Fragment) : ReadWriteProperty<Fragment, T> {
    private var _value: T? = null

    init {
        fragment.addOnDestroyObserver { _value = null }
    }

    override fun getValue(thisRef: Fragment, property: KProperty<*>): T {
        return _value ?: throw IllegalStateException(
            "should never call auto-cleared-value get when it might not be available"
        )
    }

    override fun setValue(thisRef: Fragment, property: KProperty<*>, value: T) {
        _value = value
    }

    class Nullable<T : Any>(val fragment: Fragment) : ReadWriteProperty<Fragment, T?> {
        private var _value: T? = null

        override fun getValue(thisRef: Fragment, property: KProperty<*>) = _value

        override fun setValue(thisRef: Fragment, property: KProperty<*>, value: T?) {
            _value = value
        }

    }

    companion object {
        fun Fragment.addOnDestroyObserver(onDestroy: () -> Unit) {
            lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onCreate(owner: LifecycleOwner) {
                    viewLifecycleOwnerLiveData.observe(this@addOnDestroyObserver) {
                        it?.lifecycle?.addObserver(
                            object : DefaultLifecycleObserver {
                                override fun onDestroy(owner: LifecycleOwner) {
                                    onDestroy()
                                }
                            }
                        )
                    }
                }
            })
        }

        fun <T : Any> Fragment.autoCleared() = AutoClearedValue<T>(this)
        fun <T : Any> Fragment.autoClearedNullable() = Nullable<T>(this)
    }
}