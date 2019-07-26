package com.connectycube.messenger.utilities

abstract class Converter<R, T> {
    abstract fun convertTo(response: T): R
}