package dto.models

import java.sql.Date

import generated.tables.records.AuthorRecord
import play.api.libs.json.{Json, OFormat}

import java.util
import java.util.stream.Collectors

import javax.inject.{Inject, Singleton}
import org.jooq.impl.DSL._
import org.jooq.scalaextensions.Conversions._
import org.jooq.{DSLContext, Record, Record3, Result}
import java.sql.Date

import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.Future
import scala.concurrent.ExecutionContext

import play.api.libs.json._

import slick.jdbc.JdbcProfile

case class AuthorModel(id: Int, first_name: Option[String], last_name: String, date_of_birth: Option[Date], year_of_birth:Option[Int], distinguished:Option[Int])

object AuthorModel {
  implicit val authorFormat = Json.format[AuthorModel]
}

