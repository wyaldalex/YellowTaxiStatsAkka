package com.tudux.taxi.http.swagger

import javax.ws.rs.Path
import akka.http.scaladsl.server.Route
import com.tudux.taxi.actors.TaxiCostStat
import com.tudux.taxi.actors.CostAggregatorResponse.CalculateTripDistanceCostResponse
import io.swagger.annotations._

@Path("/api/yellowtaxi/service/calculate-distance-cost/{distance}")
@Api(value = "/service")
@SwaggerDefinition(tags = Array(new Tag(name = "CalculateTripDistanceCost", description = "Operation to calculate trip cost")))
trait CalculateTripDistanceCost {
  @ApiOperation(value = "service", tags = Array("service-domain-ops"), httpMethod = "GET", notes = "This route will return the trip cost based on distance")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "distance", value = "Distance of the trip to calculate cost", required = true, dataType = "int", paramType = "path")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "OK", response = classOf[CalculateTripDistanceCostResponse]),
    new ApiResponse(code = 400, message = "The request content was malformed"),
    new ApiResponse(code = 500, message = "There was an internal server error.")
  ))
  def calculateTripDistanceCostSwagger: Option[Route] = None

}
