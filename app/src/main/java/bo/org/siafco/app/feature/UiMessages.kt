package bo.org.siafco.app.feature

import androidx.annotation.StringRes
import bo.org.siafco.app.R
import bo.org.siafco.app.core.network.ApiResult

enum class UiMessage(@param:StringRes val resId: Int) {
    Unauthorized(R.string.error_unauthorized),
    Forbidden(R.string.error_forbidden),
    Validation(R.string.error_validation),
    RateLimited(R.string.error_rate_limited),
    Network(R.string.error_network),
    Unknown(R.string.error_unknown),
    LogoutLocalSuccess(R.string.logout_local_success),
    LogoutRemoteFailed(R.string.logout_remote_failed)
}

fun ApiResult<*>.toUiMessage(): UiMessage = when (this) {
    is ApiResult.HttpError -> when (code) {
        401 -> UiMessage.Unauthorized
        403 -> UiMessage.Forbidden
        422 -> UiMessage.Validation
        429 -> UiMessage.RateLimited
        else -> UiMessage.Unknown
    }
    ApiResult.NetworkError -> UiMessage.Network
    ApiResult.UnknownError -> UiMessage.Unknown
    is ApiResult.Success -> UiMessage.Unknown
}
