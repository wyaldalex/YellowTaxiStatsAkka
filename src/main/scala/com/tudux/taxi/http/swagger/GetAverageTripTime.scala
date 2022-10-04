package com.tudux.taxi.http.swagger

import akka.http.scaladsl.server.Route
import com.tudux.taxi.actors.TaxiTripTimeResponses.TaxiTripAverageTimeMinutesResponse
import io.swagger.annotations._

import javax.ws.rs.Path

@Path("/api/yellowtaxi/service/average-trip-time")
@Api(value = "/service")
@SwaggerDefinition(tags = Array(new Tag(name = "GetAverageTripTime", description = "Operation to get Average Trip Time in NY Area")))
trait GetAverageTripTime {
  @ApiOperation(value = "service", tags = Array("service-domain-ops"), httpMethod = "GET", notes = "This route will return the average Trip Time in minutes for NY Area")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "OK", response = classOf[TaxiTripAverageTimeMinutesResponse]),
    new ApiResponse(code = 400, message = "The request content was malformed"),
    new ApiResponse(code = 500, message = "There was an internal server error.")
  ))
  def getAverageTripTimeSwagger: Option[Route] = None
}
