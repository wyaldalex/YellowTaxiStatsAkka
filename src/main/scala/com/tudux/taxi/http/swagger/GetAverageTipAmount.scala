package com.tudux.taxi.http.swagger
import javax.ws.rs.Path
import akka.http.scaladsl.server.Route
import com.tudux.taxi.actors.TaxiCostStatsResponse.GetAverageTipAmountResponse
import com.tudux.taxi.actors.TaxiTripTimeResponses.TaxiTripAverageTimeMinutesResponse
import io.swagger.annotations._
import io.swagger.models.Operation

@Path("/api/yellowtaxi/service/average-tip")
@Api(value = "/service")
@SwaggerDefinition(tags = Array(new Tag(name = "GetAverageTipAmount", description = "Operation to get Average Tip Amount in NY")))
trait GetAverageTipAmount {
  @ApiOperation(value = "service", tags = Array("service-domain-ops"), httpMethod = "GET", notes = "This route will return the average Tip amount in NY")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "OK", response = classOf[GetAverageTipAmountResponse]),
    new ApiResponse(code = 400, message = "The request content was malformed"),
    new ApiResponse(code = 500, message = "There was an internal server error.")
  ))
  def getAverageTipAmountResponseSwagger: Option[Route] = None
}
