package com.tudux.taxi.http.routes

import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.server.Directives.{complete, get, path}

case class Ping(implicit system: ActorSystem) {
  val routes = path("ping") {
    get {
      complete(HttpResponse(OK, entity = "pong"))
    }
  }
}
