package com.tudux.taxi.http.swagger

import akka.http.scaladsl.server.Route
import com.tudux.taxi.actors.common.response.CommonOperationResponse.OperationResponse
import com.tudux.taxi.actors.timeinfo.TaxiTripTimeInfo
import com.tudux.taxi.http.payloads.RoutePayloads.UpdateTimeInfoRequest
import io.swagger.annotations._
import io.swagger.models.Operation

import javax.ws.rs.Path

@Path("/api/yellowtaxi/time/{tripId}")
@Api(value = "/time")
@SwaggerDefinition(tags = Array(new Tag(name = "GetTaxiTimeStat", description = "Operation to get  TaxiTimeStat by statID")))
trait GetTaxiTimeInfoStat {

  @ApiOperation(value = "stat", tags = Array("time"), httpMethod = "GET", notes = "This route will retrieve Taxi Time Stats by Id")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "tripId", value = "The unique id of the taxi trip stat", required = true, dataType = "int", paramType = "path")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "OK", response = classOf[TaxiTripTimeInfo]),
    new ApiResponse(code = 400, message = "The request content was malformed"),
    new ApiResponse(code = 500, message = "There was an internal server error.")
  ))
  def getTaxiTimeStatSwagger: Option[Route] = None

}

@Path("/api/yellowtaxi/time/{tripId}")
@Api(value = "/time")
@SwaggerDefinition(tags = Array(new Tag(name = "UpdateTaxiTime", description = "Operation used to update Taxi Trip Time Stat")))
trait UpdateTaxiTimeInfo {

  @Operation
  @ApiOperation(value = "stat", tags = Array("time"), httpMethod = "PUT", notes = "This route will update Taxi Time for a given Trip Stat ID")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "UpdateTaxiTimeRequest", required = true,
      dataTypeClass = classOf[UpdateTimeInfoRequest], paramType = "body"),
    new ApiImplicitParam(name = "tripId", value = "The unique id of the taxi trip stat", required = true, dataType = "int", paramType = "path")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "OK", response = classOf[OperationResponse]),
    new ApiResponse(code = 400, message = "The request content was malformed"),
    new ApiResponse(code = 500, message = "There was an internal server error.")
  ))
  def updateTaxiTimeInfoSwagger: Option[Route] = None

}
