package com.github.chaabaj.openid.apis.facebook

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.{HttpRequest, Uri}
import com.github.chaabaj.openid.WebServiceApi
import com.github.chaabaj.openid.exceptions.{OAuthException, WebServiceException}
import com.github.chaabaj.openid.oauth._
import com.github.chaabaj.openid.protocol.{DataProtocol, JsonProtocol}
import spray.json.JsValue

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

trait FacebookOAuthService extends WebServiceApi[JsValue] with OAuthService {
  override val protocol: DataProtocol[JsValue] = new JsonProtocol

  import com.github.chaabaj.openid.oauth.OAuthResponseFormat._
  import com.github.chaabaj.openid.apis.facebook.FacebookErrorFormat._

  private def toOAuthError(fbError: FacebookError): OAuthError = fbError.code match {
    case 101 => OAuthError(OAuthErrorCodes.InvalidClient, fbError.message)
    case _ => OAuthError(OAuthErrorCodes.InvalidRequest, fbError.message)
  }

  override def issueOAuthToken(authorizationCode: String, redirectUri: String)(implicit exc: ExecutionContext): Future[OAuthTokenIssuing] = {
    val httpRequest = HttpRequest(
      uri = Uri("https://graph.facebook.com/v2.8/oauth/access_token")
        .withQuery(
          Query(
            "client_id" -> config.clientId,
            "client_secret" -> config.clientSecret,
            "redirect_uri" -> redirectUri,
            "code" -> authorizationCode
          )
        )
    )

    request(httpRequest)
      .map(_.convertTo[OAuthTokenIssuing])
      .recover {
        case t @ WebServiceException(statusCode, jsonError: JsValue) =>
          if (jsonError.asJsObject.getFields("error").nonEmpty) {
            val fbError = jsonError.asJsObject.getFields("error").head.convertTo[FacebookError]

            if (fbError.`type` == "OAuthException") {
              throw OAuthException(statusCode, toOAuthError(fbError))
            } else {
              throw t
            }
          } else {
            throw t
          }
        case t: Throwable => throw t
      }
  }
}

private class FacebookOAuthServiceImpl(val config: OAuthConfig)
                                      (implicit val actorSystem: ActorSystem,
                                       implicit val timeout: FiniteDuration) extends FacebookOAuthService

object FacebookOAuthService {
  def apply(config: OAuthConfig)(implicit actorSystem: ActorSystem, timeout: FiniteDuration): OAuthService =
    new FacebookOAuthServiceImpl(config)
}