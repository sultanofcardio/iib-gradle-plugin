@file:Suppress("MemberVisibilityCanBePrivate")

package com.sultanofcardio.iib.plugin.models

enum class ValidateMaster(val value: String) {
    None("None"),
    ContentAndValue("Content and Value"),
    Content("Content"),
    Inherit("Inherit");

    override fun toString(): String = value
}