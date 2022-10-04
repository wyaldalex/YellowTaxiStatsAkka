package com.tudux.taxi.http.swagger

import javax.ws.rs.Path
import akka.http.scaladsl.server.Route
import io.swagger.annotations._
import io.swagger.models.Operation

@Path("/api/yellowtaxi/stat/{statId}")
@Api(value = "/stat")
@SwaggerDefinition(tags = Array(new Tag(name = "DeleteTaxiTripStat", description = "Operation to Delete Stat by statID")))
trait DeleteTaxiStat {
  @ApiOperation(value = "stat", tags = Array("stat"), httpMethod = "DELETE", notes = "This route will delete Trip stat by Id"   )
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "statId", value = "The unique id of the taxi trip stat", required = true, dataType = "int", paramType = "path")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "OK"),
    new ApiResponse(code = 400, message = "The request content was malformed"),
    new ApiResponse(code = 500, message = "There was an internal server error.")
  ))
  def deleteTaxiTripStatSwagger: Option[Route] = None

}
