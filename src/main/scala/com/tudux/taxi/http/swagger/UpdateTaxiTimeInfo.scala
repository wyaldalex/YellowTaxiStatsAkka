package com.tudux.taxi.http.swagger

import javax.ws.rs.Path
import akka.http.scaladsl.server.Route
import com.tudux.taxi.actors.TaxiTripTimeInfoStat
import com.tudux.taxi.http.{CreateTaxiStatRequest, UpdateTimeInfoRequest}
import io.swagger.annotations._
import io.swagger.models.Operation

@Path("/api/yellowtaxi/time/{statId}")
@Api(value = "/time")
@SwaggerDefinition(tags = Array(new Tag(name = "UpdateTaxiTime", description = "Operation used to update Taxi Trip Time Stat")))
trait UpdateTaxiTimeInfo {

  @Operation
  @ApiOperation(value = "stat", tags = Array("time"), httpMethod = "PUT", notes = "This route will update Taxi Time for a given Trip Stat ID")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "UpdateTaxiTimeRequest", required = true,
      dataTypeClass = classOf[UpdateTimeInfoRequest], paramType = "body"),
    new ApiImplicitParam(name = "statId", value = "The unique id of the taxi trip stat", required = true, dataType = "int", paramType = "path")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "OK"),
    new ApiResponse(code = 400, message = "The request content was malformed"),
    new ApiResponse(code = 500, message = "There was an internal server error.")
  ))
  def updateTaxiTimeInfoSwagger: Option[Route] = None

}
