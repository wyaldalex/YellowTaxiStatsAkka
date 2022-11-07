package com.tudux.taxi.http.swagger

import akka.http.scaladsl.server.Route
import com.tudux.taxi.actors.aggregators.CostAggregatorResponse.GetAverageTipAmountResponse
import io.swagger.annotations._

import javax.ws.rs.Path

@Path("/api/yellowtaxi/service/average-tip")
@Api(value = "/service")
@SwaggerDefinition(tags = Array(new Tag(name = "GetAverageTipAmount", description = "Operation to get Average Tip Amount in NY")))
trait GetAverageTipAmount {
  @ApiOperation(value = "service", tags = Array("service"), httpMethod = "GET", notes = "This route will return the average Tip amount in NY")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "OK", response = classOf[GetAverageTipAmountResponse]),
    new ApiResponse(code = 400, message = "The request content was malformed"),
    new ApiResponse(code = 500, message = "There was an internal server error.")
  ))
  def getAverageTipAmountResponseSwagger: Option[Route] = None
}

import com.tudux.taxi.actors.aggregators.TimeAggregatorResponse.TaxiTripAverageTimeMinutesResponse

@Path("/api/yellowtaxi/service/average-trip-time")
@Api(value = "/service")
@SwaggerDefinition(tags = Array(new Tag(name = "GetAverageTripTime", description = "Operation to get Average Trip Time in NY Area")))
trait GetAverageTripTime {
  @ApiOperation(value = "service", tags = Array("service"), httpMethod = "GET", notes = "This route will return the average Trip Time in minutes for NY Area")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "OK", response = classOf[TaxiTripAverageTimeMinutesResponse]),
    new ApiResponse(code = 400, message = "The request content was malformed"),
    new ApiResponse(code = 500, message = "There was an internal server error.")
  ))
  def getAverageTripTimeSwagger: Option[Route] = None
}

import com.tudux.taxi.actors.aggregators.CostAggregatorResponse.CalculateTripDistanceCostResponse

@Path("/api/yellowtaxi/service/calculate-distance-cost/{distance}")
@Api(value = "/service")
@SwaggerDefinition(tags = Array(new Tag(name = "CalculateTripDistanceCost", description = "Operation to calculate trip cost")))
trait CalculateTripDistanceCost {
  @ApiOperation(value = "service", tags = Array("service"), httpMethod = "GET", notes = "This route will return the trip cost based on distance")
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

/*
import com.tudux.taxi.http.payloads.RoutePayloads.LoadedStatsResponse

@Path("/api/yellowtaxi/actor/loaded")
@Api(value = "/loaded")
@SwaggerDefinition(tags = Array(new Tag(name = "GetLoadedStats", description = "Operation to get Actor Loaded Stats")))
trait GetLoadedStats {
  @ApiOperation(value = "service", tags = Array("actor-info"), httpMethod = "GET", notes = "This route will return the loaded stats per actor")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "OK", response = classOf[LoadedStatsResponse]),
    new ApiResponse(code = 400, message = "The request content was malformed"),
    new ApiResponse(code = 500, message = "There was an internal server error.")
  ))
  def getAverageTripTimeSwagger: Option[Route] = None
} */
