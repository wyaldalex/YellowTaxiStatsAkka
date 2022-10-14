package com.tudux.taxi.http.validation

import cats.data.ValidatedNel
import cats.implicits._

object Validation {

  trait Required[A] extends (A => Boolean)

  trait ValidationFailure {
    def errorMessage: String
  }

  type ValidationResult[A] = ValidatedNel[ValidationFailure, A]
  trait Minimum[A] extends ((A, Double) => Boolean)

  implicit val requiredString: Required[String] = _.nonEmpty
  implicit val requiredInt: Required[Int] = _.isValidInt
  implicit val requiredDouble: Required[Double] = !_.isNaN
  implicit val minimumInt: Minimum[Int] = _ >= _
  implicit val minimumDouble: Minimum[Double] = _ >= _

  def minimum[A](value: A, threshold: Double)(implicit min: Minimum[A]): Boolean = min(value, threshold)
  def required[A](value: A)(implicit req: Required[A]): Boolean = req(value)

  case class EmptyField(fieldName: String) extends ValidationFailure {
    override def errorMessage: String = s"$fieldName is empty"
  }

  case class NegativeValue(fieldName: String) extends ValidationFailure {
    override def errorMessage: String = s"$fieldName is negative"
  }

  case class BelowMinimumValue(fieldName: String, min: Double) extends ValidationFailure {
    override def errorMessage: String = s"$fieldName is below the minimum threshold $min"
  }

  def validateRequired[A: Required](value: A, fieldName: String): ValidationResult[A] = {
    if (required(value)) value.validNel
    else EmptyField(fieldName).invalidNel
  }

  def validateMinimum[A: Minimum](value: A, threshold: Double, fieldName: String): ValidationResult[A] = {
    if (minimum(value, threshold)) value.validNel
    else if (threshold == 0) NegativeValue(fieldName).invalidNel
    else BelowMinimumValue(fieldName, threshold).invalidNel
  }

  trait Validator[A] {
    def validate(value: A): ValidationResult[A]
  }

  def validateEntity[A](value: A)(implicit validator: Validator[A]): ValidationResult[A] = {
    validator.validate(value)
  }

}
