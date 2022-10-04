package com.tudux.taxi.http.swagger

import javax.ws.rs.Path
import akka.http.scaladsl.server.Route
import com.tudux.taxi.actors.TaxiCostStat
import io.swagger.annotations._
import io.swagger.models.Operation

@Path("/api/yellowtaxi/cost/{statId}")
@Api(value = "/cost")
@SwaggerDefinition(tags = Array(new Tag(name = "GetTaxiCostStat", description = "Operation to get  TaxiCostStat by statID")))
trait GetTaxiCostStat {
  @ApiOperation(value = "stat", tags = Array("stat"), httpMethod = "GET", notes = "This route will retrieve Taxi Trip Cost by Id"   )
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "statId", value = "The unique id of the taxi trip stat", required = true, dataType = "int", paramType = "path")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "OK", response = classOf[TaxiCostStat]),
    new ApiResponse(code = 400, message = "The request content was malformed"),
    new ApiResponse(code = 500, message = "There was an internal server error.")
  ))
  def getTaxiCostStatSwagger: Option[Route] = None

}
