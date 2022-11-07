package com.tudux.taxi.actors.common.response

sealed trait CommonResponse
object CommonOperationResponse {

  case class OperationResponse(id: String, status: Either[String,String],
                               message: Either[String,String] = Right("Successfully persisted")) extends CommonResponse

}
