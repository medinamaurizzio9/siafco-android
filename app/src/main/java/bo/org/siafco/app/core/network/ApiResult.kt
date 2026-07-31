package bo.org.siafco.app.core.network

sealed interface ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>
    data class HttpError(val code: Int, val message: String?) : ApiResult<Nothing>
    data object NetworkError : ApiResult<Nothing>
    data object UnknownError : ApiResult<Nothing>
}
