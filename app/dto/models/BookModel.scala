package dto.models

import generated.tables.records.{AuthorRecord, BookRecord}
import play.api.libs.json.{Json, OFormat}

case class BookModel(
                      id:Int,
                      authors: Seq[AuthorModel])

object BookModel {
  implicit val book_format : OFormat[BookModel] = Json.format[BookModel]
}