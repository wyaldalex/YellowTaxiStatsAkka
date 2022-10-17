package com.tudux.taxi.http.validation

import cats.data.ValidatedNel
import cats.implicits._

import scala.util.{Success, Try}

object Validation {

  trait Required[A] extends (A => Boolean)

  trait ValidationFailure {
    def errorMessage: String
  }

  type ValidationResult[A] = ValidatedNel[ValidationFailure, A]
  trait Minimum[A] extends ((A, Double) => Boolean)
  trait MinimumStr[A] extends ((A, String) => Boolean)

  implicit val requiredString: Required[String] = _.nonEmpty
  implicit val requiredInt: Required[Int] = _.isValidInt
  implicit val requiredDouble: Required[Double] = !_.isNaN
  implicit val minimumInt: Minimum[Int] = _ >= _
  implicit val minimumDouble: Minimum[Double] = _ >= _
  implicit val minimumStr: MinimumStr[String] = _ >= _

  def minimum[A](value: A, threshold: Double)(implicit min: Minimum[A]): Boolean = min(value, threshold)
  def required[A](value: A)(implicit req: Required[A]): Boolean = req(value)

  case class EmptyField(fieldName: String) extends ValidationFailure {
    override def errorMessage: String = s"$fieldName is empty"
  }

  case class NegativeValue(fieldName: String) extends ValidationFailure {
    override def errorMessage: String = s"$fieldName is negative"
  }

  case class DateBadFormat(date: String) extends ValidationFailure {
    override def errorMessage: String = s"$date has incorrect format"
  }

  case class EndDateIsLessThanStartDate(fieldNameStart: String, fieldNameEnd: String) extends ValidationFailure {
    override def errorMessage: String = s"$fieldNameStart is more recent than $fieldNameEnd"
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

  def validatePairDates[A: MinimumStr](value: A,date1: String, date2: String, fieldName: String, fieldName2 : String) : ValidationResult[A] = {
    val format = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:SS")

    val tryParseDate1 = Try(format.parse(date1))
    val tryParseDate2 = Try(format.parse(date2))
    if (tryParseDate1.isSuccess && tryParseDate2.isSuccess) {
      val date1Minutes = format.parse(date1).getTime
      val date2Minutes = format.parse(date2).getTime
      if (date1Minutes < date2Minutes) value.validNel
      else EndDateIsLessThanStartDate(fieldName, fieldName2).invalidNel
    } else {
      if(tryParseDate1.isFailure) DateBadFormat(fieldName).invalidNel
      else DateBadFormat(fieldName2).invalidNel
    }
  }

  trait Validator[A] {
    def validate(value: A): ValidationResult[A]
  }

  def validateEntity[A](value: A)(implicit validator: Validator[A]): ValidationResult[A] = {
    validator.validate(value)
  }
}
