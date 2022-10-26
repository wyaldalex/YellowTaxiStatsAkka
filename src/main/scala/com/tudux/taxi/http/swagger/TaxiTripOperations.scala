package com.tudux.taxi.http.swagger

import akka.http.scaladsl.server.Route
import com.tudux.taxi.http.payloads.RoutePayloads.{CombinedTaxiTripOperationResponse, CreateTaxiTripRequest}
import io.swagger.annotations._
import io.swagger.models.Operation

import javax.ws.rs.Path

@Path("/api/yellowtaxi/taxitrip")
@Api(value = "/taxitrip")
@SwaggerDefinition(tags = Array(new Tag(name = "CreateTaxiTripStat", description = "Operation used to create Taxi Trip Stat")))
trait CreateStatResponse {
  @Operation
  @ApiOperation(value = "taxitrip", tags = Array("taxitrip"), httpMethod = "POST", notes = "This route will return the id of the created stat"   )
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "CreateTaxiStatRequest", required = true,
      dataTypeClass = classOf[CreateTaxiTripRequest] , paramType = "body")
  ))
  @ApiResponses(Array(
    //new ApiResponse(code = 201, message = "Taxi Trip created with Id: {taxiId}"),
    new ApiResponse(code = 200, message = "OK", response = classOf[CombinedTaxiTripOperationResponse]),
    new ApiResponse(code = 400, message = "The request content was malformed"),
    new ApiResponse(code = 500, message = "There was an internal server error.")
  ))
  def createTaxiTripStatSwagger: Option[Route] = None
}

@Path("/api/yellowtaxi/taxitrip/{tripId}")
@Api(value = "/taxitrip")
@SwaggerDefinition(tags = Array(new Tag(name = "DeleteTaxiTripStat", description = "Operation to Delete Stat by statID")))
trait DeleteTaxiStat {
  @ApiOperation(value = "taxitrip", tags = Array("taxitrip"), httpMethod = "DELETE", notes = "This route will delete Trip stat by Id"   )
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "tripId", value = "The unique id of the taxi trip stat", required = true, dataType = "int", paramType = "path")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "OK", response = classOf[CombinedTaxiTripOperationResponse]),
    new ApiResponse(code = 400, message = "The request content was malformed"),
    new ApiResponse(code = 500, message = "There was an internal server error.")
  ))
  def deleteTaxiTripStatSwagger: Option[Route] = None

}
