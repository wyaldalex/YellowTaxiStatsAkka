package com.tudux.taxi.http.swagger

import akka.http.scaladsl.server.Route
import com.tudux.taxi.http.RouteHelpers.UpdateCostInfoRequest
import io.swagger.annotations._
import io.swagger.models.Operation

import javax.ws.rs.Path

@Path("/api/yellowtaxi/cost/{statId}")
@Api(value = "/cost")
@SwaggerDefinition(tags = Array(new Tag(name = "UpdateTaxiCost", description = "Operation used to update Taxi Trip Cost Stat")))
trait UpdateTaxiCost {

  @Operation
  @ApiOperation(value = "stat", tags = Array("cost"), httpMethod = "PUT", notes = "This route will update Taxi Cost for a given Trip Stat ID")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "UpdateTaxiCostRequest", required = true,
      dataTypeClass = classOf[UpdateCostInfoRequest], paramType = "body"),
    new ApiImplicitParam(name = "statId", value = "The unique id of the taxi trip stat", required = true, dataType = "int", paramType = "path")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "OK"),
    new ApiResponse(code = 400, message = "The request content was malformed"),
    new ApiResponse(code = 500, message = "There was an internal server error.")
  ))
  def updateTaxiCostSwagger: Option[Route] = None

}
