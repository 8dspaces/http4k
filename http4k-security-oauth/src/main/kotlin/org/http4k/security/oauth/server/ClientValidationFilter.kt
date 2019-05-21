package org.http4k.security.oauth.server

import com.natpryce.get
import com.natpryce.map
import com.natpryce.mapFailure
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.then
import org.http4k.filter.ServerFilters
import org.http4k.security.ResponseType

class ClientValidationFilter(private val clientValidator: ClientValidator,
                             private val errorRenderer: ErrorRenderer,
                             private val extractor: AuthRequestExtractor) : Filter {

    override fun invoke(next: HttpHandler): HttpHandler =
        ServerFilters.CatchLensFailure
            .then {
                if (!validResponseTypes.contains(it.query("response_type"))) {
                    return@then errorRenderer.response(UnsupportedResponseType(it.query("response_type").orEmpty()))
                }
                extractor.extract(it).map { authorizationRequest ->
                    if (!clientValidator.validateClientId(authorizationRequest.client)) {
                        errorRenderer.response(InvalidClientId)
                    } else if (!clientValidator.validateRedirection(authorizationRequest.client, authorizationRequest.redirectUri)) {
                        errorRenderer.response(InvalidRedirectUri)
                    } else {
                        next(it)
                    }
                }.mapFailure(errorRenderer::response).get()
            }

    companion object {
        val validResponseTypes = ResponseType.values().map { it.queryParameterValue }
    }
}