package dto.models

import generated.tables.records.{AuthorRecord, BookRecord}
import play.api.libs.json.{Json, OFormat}

case class BooksWithAuthorsModel(
                             id:Int,
                             authors: Seq[AuthorModel]
                           )

object BooksWithAuthorsModel {
  implicit val bookWithAuthor_format : OFormat[BooksWithAuthorsModel] = Json.format[BooksWithAuthorsModel]
}
