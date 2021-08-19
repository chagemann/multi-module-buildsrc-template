package com.myapplication.plugin

import org.gradle.api.Action

open class MyPluginOptionExtension {
    open val jacoco: JacocoOptions = JacocoOptions()

    open fun jacoco(action: Action<JacocoOptions>)   {
        action.execute(jacoco)
    }
}

class JacocoOptions {
    var isEnabled: Boolean = true

    var excludes: ArrayList<String> = arrayListOf()
    fun excludes(vararg excludes: String) {
        this.excludes.addAll(excludes)
    }
}