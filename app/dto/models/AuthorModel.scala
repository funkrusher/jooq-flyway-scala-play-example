package dto.models

import generated.tables.records.AuthorRecord
import play.api.libs.json.{Json, OFormat}

case class AuthorModel(id:Int);


object AuthorModel {
  implicit val author_format : OFormat[AuthorModel] = Json.format[AuthorModel]
}