package controllers

import javax.inject._

import models.AccessToken
import play.api._
import play.api.libs.json.Json
import play.api.mvc._
import play.api.libs.ws._
import play.api.Configuration

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject() (ws: WSClient,conf : Configuration) extends Controller {

  /**
   * Create an Action to render an HTML page with a welcome message.
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def login = Action {
    val url = conf.getString("drops.url.base").get + conf.getString("drops.url.code").get +
      conf.getString("drops.client_id").get //+ "/" + conf.getString("drops.client_secret").get
    Redirect(url)
  }

  def receiveCode(code: String) = Action.async {
    val url = conf.getString("drops.url.base").get + conf.getString("drops.url.accessToken").get
    val clientId = conf.getString("drops.client_id").get
//    val clientSecret = conf.getString("drops.client_secret").get

    val accessToken = ws.url(url).withQueryString(
      "grant_type" -> "authorization_code",
      "client_id" -> clientId,
//      "client_secret" -> clientSecret,
      "code" -> code,
      "redirect_uri" -> "http://localhost:8000/endpoint?code="
    ).get().map(response => response.status match {
      case 200 => AccessToken(response.json)
      case _ => println(response.status);throw new Exception // Todo: throw meaningful exception considering the returned error message and status code!
    })

    accessToken.flatMap(token => {
      val url = conf.getString("drops.url.base").get + conf.getString("drops.url.profile").get

      ws.url(url).withQueryString(
        "access_token" -> token.content
      ).get().map(response => response.status match {
        case 200 => Ok(Json.obj("status" -> "success", "code" -> code, "token" -> token.content, "user" -> response.json))
        case _ => Ok(Json.obj("status" -> "error", "code" -> code, "token" -> token.content, "response-status" -> response.status))
      })
    })
  }
}
