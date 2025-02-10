package dev.brahmkshatriya.echo.extensions.plugger

import dalvik.system.DexClassLoader
import java.lang.ref.WeakReference

class ClassLoaderWithPreserved(
    private val preservedPackages: List<String>,
    dexPath: String,
    optimizedDirectory: String?,
    librarySearchPath: String,
    parent: ClassLoader
) : DexClassLoader(dexPath, optimizedDirectory, librarySearchPath, parent) {

    override fun loadClass(name: String?, resolve: Boolean): Class<*> {
        if (name != null && preservedPackages.any { name.startsWith(it) }) {
            val loadedClass = classMap[name]?.get()
            if (loadedClass != null) return loadedClass
            val clazz = super.loadClass(name, resolve)
            classMap[name] = WeakReference(clazz)
            return clazz
        }
        return super.loadClass(name, resolve)
    }

    companion object {
        private val classMap = mutableMapOf<String, WeakReference<Class<*>>>()
    }
}