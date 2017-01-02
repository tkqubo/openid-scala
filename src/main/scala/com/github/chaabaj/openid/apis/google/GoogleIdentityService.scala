package com.github.chaabaj.openid.apis.google

import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.headers.RawHeader
import com.github.chaabaj.openid.WebServiceApi
import com.github.chaabaj.openid.oauth.OAuthTokenIssuing
import com.github.chaabaj.openid.openid.IdentityService
import com.github.chaabaj.openid.protocol.{DataProtocol, JsonProtocol}
import spray.json.JsValue
import spray.json.DefaultJsonProtocol._

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

trait GoogleIdentityService extends WebServiceApi[JsValue] with IdentityService {
  override val protocol: DataProtocol[JsValue] = new JsonProtocol

  override def getIdentity(token: OAuthTokenIssuing)
                          (implicit exc: ExecutionContext): Future[String] = {
    val httpRequest = HttpRequest(
      uri = "https://www.googleapis.com/userinfo/v2/me"
    ).withHeaders(RawHeader("Authorization", s"Bearer ${token.accessToken}"))

    request(httpRequest)
      .map(_.asJsObject.getFields("email").head.convertTo[String])
  }
}

private class GoogleIdentityServiceImpl(
  implicit val actorSystem: ActorSystem,
  implicit val timeout: FiniteDuration
) extends GoogleIdentityService

object GoogleIdentityService {
  def apply()(implicit actorSystem: ActorSystem, timeout: FiniteDuration): IdentityService =
    new GoogleIdentityServiceImpl()
}