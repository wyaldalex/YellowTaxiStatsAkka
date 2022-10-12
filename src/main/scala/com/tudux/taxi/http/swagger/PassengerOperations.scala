package com.tudux.taxi.http.swagger

import akka.http.scaladsl.server.Route
import com.tudux.taxi.actors.TaxiTripPassengerInfoStat
import io.swagger.annotations._

import javax.ws.rs.Path

@Path("/api/yellowtaxi/passenger/{statId}")
@Api(value = "/passenger")
@SwaggerDefinition(tags = Array(new Tag(name = "GetTaxiPassengerStat", description = "Operation to get  TaxiPassengerStat by statID")))
trait GetTaxiPassenger {
  @ApiOperation(value = "stat", tags = Array("passenger"), httpMethod = "GET", notes = "This route will retrieve Taxi Trip Passenger Info by Id"   )
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "statId", value = "The unique id of the taxi trip stat", required = true, dataType = "int", paramType = "path")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "OK", response = classOf[TaxiTripPassengerInfoStat]),
    new ApiResponse(code = 400, message = "The request content was malformed"),
    new ApiResponse(code = 500, message = "There was an internal server error.")
  ))
  def getTaxiPassengerStatSwagger: Option[Route] = None

}

import com.tudux.taxi.http.RouteHelpers.UpdatePassengerInfoRequest
import io.swagger.models.Operation



@Path("/api/yellowtaxi/passenger/{statId}")
@Api(value = "/passenger")
@SwaggerDefinition(tags = Array(new Tag(name = "UpdateTaxipassenger", description = "Operation used to update Taxi Trip passenger Stat")))
trait UpdateTaxiPassenger {

  @Operation
  @ApiOperation(value = "stat", tags = Array("passenger"), httpMethod = "PUT", notes = "This route will update Taxi passenger for a given Trip Stat ID")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "UpdateTaxipassengerRequest", required = true,
      dataTypeClass = classOf[UpdatePassengerInfoRequest], paramType = "body"),
    new ApiImplicitParam(name = "statId", value = "The unique id of the taxi trip stat", required = true, dataType = "int", paramType = "path")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "OK"),
    new ApiResponse(code = 400, message = "The request content was malformed"),
    new ApiResponse(code = 500, message = "There was an internal server error.")
  ))
  def updateTaxipassengerSwagger: Option[Route] = None

}
