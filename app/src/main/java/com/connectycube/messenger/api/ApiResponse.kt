package com.connectycube.messenger.api

import android.os.Bundle

/**
 * Common class used by API responses.
 * @param <T> the type of the response object
</T> */
@Suppress("unused") // T is used in extending classes
sealed class ApiResponse<T> {
    companion object {
        fun <T> create(error: Throwable): ApiErrorResponse<T> {
            return ApiErrorResponse(error.message ?: "unknown error")
        }

        fun <T> create(response: T): ApiResponse<T> {
            return ApiSuccessResponse(response)
        }

        fun <T> create(response: T, responseBundle: Bundle?): ApiResponse<T> {
            return ApiSuccessResponse(response, responseBundle)
        }

        fun <T> create(progress: Int): ApiProgressResponse<T> {
            return ApiProgressResponse(progress)
        }
    }
}


class ApiEmptyResponse<T> : ApiResponse<T>()

class ApiProgressResponse<T>(val progress: Int) : ApiResponse<T>()

data class ApiSuccessResponse<T>(
    val body: T
) : ApiResponse<T>() {
    var bundle: Bundle? = null

    constructor(body: T, bundle: Bundle?) : this(body) {
        this.bundle = bundle
    }
}

data class ApiErrorResponse<T>(val errorMessage: String) : ApiResponse<T>()
