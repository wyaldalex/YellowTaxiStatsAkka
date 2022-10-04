package com.tudux.taxi.http.swagger

import akka.http.scaladsl.server.Route
import com.tudux.taxi.actors.TaxiTripTimeInfoStat
import io.swagger.annotations._

import javax.ws.rs.Path

@Path("/api/yellowtaxi/time/{statId}")
@Api(value = "/time")
@SwaggerDefinition(tags = Array(new Tag(name = "GetTaxiTimeStat", description = "Operation to get  TaxiTimeStat by statID")))
trait GetTaxiTimeInfoStat {

  @ApiOperation(value = "stat", tags = Array("time"), httpMethod = "GET", notes = "This route will retrieve Taxi Time Stats by Id")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "statId", value = "The unique id of the taxi trip stat", required = true, dataType = "int", paramType = "path")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "OK", response = classOf[TaxiTripTimeInfoStat]),
    new ApiResponse(code = 400, message = "The request content was malformed"),
    new ApiResponse(code = 500, message = "There was an internal server error.")
  ))
  def getTaxiTimeStatSwagger: Option[Route] = None

}
