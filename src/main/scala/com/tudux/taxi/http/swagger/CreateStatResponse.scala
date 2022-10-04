package com.tudux.taxi.http.swagger

import javax.ws.rs.Path
import akka.http.scaladsl.server.Route
import com.tudux.taxi.http.CreateTaxiStatRequest
import io.swagger.annotations._
import io.swagger.models.Operation

@Path("/api/yellowtaxi")
@Api(value = "/stat")
@SwaggerDefinition(tags = Array(new Tag(name = "CreateTaxiTripStat", description = "Operation used to create Taxi Trip Stat")))
trait CreateStatResponse {
  @Operation
  @ApiOperation(value = "stat", tags = Array("stat"), httpMethod = "POST", notes = "This route will return the id of the created stat"   )
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "CreateStatRequest", required = true,
      dataTypeClass = classOf[CreateTaxiStatRequest] , paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "OK"),
    new ApiResponse(code = 500, message = "There was an internal server error.")
  ))
  def pingSwagger: Option[Route] = None
}
