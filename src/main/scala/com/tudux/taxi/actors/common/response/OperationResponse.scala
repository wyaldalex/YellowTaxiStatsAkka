package com.tudux.taxi.actors.common.response

sealed trait CommonResponse
object CommonOperationResponse {

  case class OperationResponse(id: String, status: String = "Success",
                               message: String = "Successfully persisted") extends CommonResponse
}
