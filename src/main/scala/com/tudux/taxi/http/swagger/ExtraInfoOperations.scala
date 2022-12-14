package com.tudux.taxi.http.swagger

import akka.http.scaladsl.server.Route
import com.tudux.taxi.actors.common.response.CommonOperationResponse.OperationResponse
import com.tudux.taxi.actors.extrainfo.TaxiTripExtraInfo
import io.swagger.annotations._

import javax.ws.rs.Path

@Path("/api/yellowtaxi/extrainfo/{tripId}")
@Api(value = "/extrainfo")
@SwaggerDefinition(tags = Array(new Tag(name = "GetTaxiextrainfoStat", description = "Operation to get  TaxiExtrainfoStat by statID")))
trait GetTaxiExtraInfo {
  @ApiOperation(value = "stat", tags = Array("extrainfo"), httpMethod = "GET", notes = "This route will retrieve Taxi Trip Extra Info by Id"   )
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "tripId", value = "The unique id of the taxi trip stat", required = true, dataType = "int", paramType = "path")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "OK", response = classOf[TaxiTripExtraInfo]),
    new ApiResponse(code = 400, message = "The request content was malformed"),
    new ApiResponse(code = 500, message = "There was an internal server error.")
  ))
  def getTaxiextrainfoStatSwagger: Option[Route] = None

}

import com.tudux.taxi.http.payloads.RoutePayloads.UpdateExtraInfoRequest
import io.swagger.models.Operation

@Path("/api/yellowtaxi/extrainfo/{tripId}")
@Api(value = "/extrainfo")
@SwaggerDefinition(tags = Array(new Tag(name = "UpdateTaxiextrainfo", description = "Operation used to update Taxi Trip extrainfo Stat")))
trait UpdateTaxiExtraInfo {

  @Operation
  @ApiOperation(value = "stat", tags = Array("extrainfo"), httpMethod = "PUT", notes = "This route will update Taxi extrainfo for a given Trip Stat ID")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "UpdateTaxiextrainfoRequest", required = true,
      dataTypeClass = classOf[UpdateExtraInfoRequest], paramType = "body"),
    new ApiImplicitParam(name = "tripId", value = "The unique id of the taxi trip stat", required = true, dataType = "int", paramType = "path")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "OK", response = classOf[OperationResponse]),
    new ApiResponse(code = 400, message = "The request content was malformed"),
    new ApiResponse(code = 500, message = "There was an internal server error.")
  ))
  def updateTaxiextrainfoSwagger: Option[Route] = None

}
