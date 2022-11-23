package com.tudux.taxi.http

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.GivenWhenThen
import org.scalatest.featurespec.AnyFeatureSpecLike
import org.scalatest.matchers.should.Matchers

class CostRouterSpec extends AnyFeatureSpecLike with GivenWhenThen with Matchers with ScalatestRouteTest
  with SprayJsonSupport {

}
