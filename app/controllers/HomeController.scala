package controllers

import javax.inject._

import models.AccessToken
import play.api._
import play.api.libs.json.Json
import play.api.mvc._
import play.api.libs.ws._

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject() (ws: WSClient) extends Controller {

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
    Redirect("http://localhost:9000/oauth2/code/get/Test/Test")
  }

  def receiveCode(code: String) = Action.async {
    val accessToken = ws.url("http://localhost:9000/oauth2/access_token").withQueryString(
      "grant_type" -> "authorization_code",
      "client_id" -> "Test",
      "client_secret" -> "Test",
      "code" -> code
    ).get().map(response => response.status match {
      case 200 => AccessToken(response.json)
      case _ => throw new Exception // Todo: throw meaningful exception considering the returned error message and status code!
    })

    accessToken.flatMap(token => {
      println(token.content)
      ws.url("http://localhost:9000/oauth2/rest/profile").withQueryString(
        "access_token" -> token.content
      ).get().map(response => response.status match {
        case 200 => Ok(Json.obj("status" -> "success", "code" -> code, "token" -> token.content, "user" -> response.json))
        case _ => Ok(Json.obj("status" -> "error", "code" -> code, "token" -> token.content, "response-status" -> response.status))
      })
    })
  }
}
