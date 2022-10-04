package com.tudux.taxi.http.swagger

import akka.http.scaladsl.server.Route
import com.tudux.taxi.http.LoadedStatsResponse
import io.swagger.annotations._

import javax.ws.rs.Path

@Path("/api/yellowtaxi/loaded/stat")
@Api(value = "/loaded")
@SwaggerDefinition(tags = Array(new Tag(name = "GetLoadedStats", description = "Operation to get Actor Loaded Stats")))
trait GetLoadedStats {
  @ApiOperation(value = "service", tags = Array("loaded-stats-per-actor"), httpMethod = "GET", notes = "This route will return the loaded stats per actor")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "OK", response = classOf[LoadedStatsResponse]),
    new ApiResponse(code = 400, message = "The request content was malformed"),
    new ApiResponse(code = 500, message = "There was an internal server error.")
  ))
  def getAverageTripTimeSwagger: Option[Route] = None
}