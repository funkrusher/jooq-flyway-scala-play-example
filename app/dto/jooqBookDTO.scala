package dto

import java.util.stream.Collectors

import database.DB
import generated.Tables._
import generated.tables.records.BookRecord
import javax.inject.{Inject, Singleton}
import org.jooq.impl.DSL.select
import org.jooq.{Record, Record3, Result}
import org.jooq.scalaextensions.Conversions._

import collection.JavaConverters._
import scala.concurrent.Future

@Singleton
class jooqBookDTO @Inject()(db: DB) {


  /**
   * returns a list of book-records
   *
   * @return list
   */
  def fetchAll(): Future[Result[BookRecord]] = db.withTransaction { dsl =>
    dsl
      .selectFrom(BOOK)
      .fetch();
  }

  /**
   * returns one book-record by its id
   *
   * @param id
   * @return book
   */
  def fetchOneById(id: Int): Future[BookRecord] = db.withTransaction { dsl =>
    dsl
      .selectFrom(BOOK)
      .where(BOOK.ID.eq(id))
      .fetchOne();
  }

  /**
   * insert one book-record
   *
   * @param book
   * @return book with auto-generated id
   */
  def insertOne(book: BookRecord): Future[BookRecord] = db.withTransaction { dsl =>
    dsl
      .insertInto(BOOK)
      .set(book)
      .returning(BOOK.ID)
      .fetchOne()
      .map({
        result =>
          book.setId(result.get(BOOK.ID));
          book;
      })
  }

  /**
   * insert book-records
   *
   * @param books
   * @return insert-counts
   */
  def insertMany(books: Seq[BookRecord]): Future[Array[Int]] = db.withTransaction { dsl =>
    dsl
      .batchInsert(books.asJava)
      .execute()
  }

  /**
   * delete book-records
   *
   * @param books
   * @return delete-counts
   */
  def deleteMany(books: Seq[BookRecord]): Future[Int] = db.withTransaction { dsl =>
    dsl
      .delete(BOOK).where(BOOK.ID.in(books.map(x => x.getId).asJava))
      .execute()
  }


  /**
   * update one book-record by its id
   *
   * @param book
   * @return book
   */
  def updateOne(book: BookRecord): Future[BookRecord] = db.withTransaction { dsl =>
    dsl
      .update(BOOK)
      .set(book)
      .where(BOOK.ID.eq(book.getId))
      .returning(BOOK.ID)
      .fetchOne();
  }

  /**
   * delete one book-record by its id
   *
   * @param book
   * @return delete-count (1)
   */
  def deleteOne(book: BookRecord): Future[Int] = db.withTransaction { dsl =>
    dsl
      .delete(BOOK)
      .where(BOOK.ID.eq(book.getId))
      .execute()
  }


  /**
   * upsert a book (meaning: inserting it if it does not exist, updating it if exists already)
   *
   * @param book
   * @return upserted book
   */
  def upsertOne(book: BookRecord): Future[Int] = db.withTransaction { dsl =>
    dsl.insertInto(BOOK)
      .set(book)
      .onDuplicateKeyUpdate()
      .set(book)
      .execute();
  }


  /**
   * complex fetch query with join, that returns only some specific columns and use many features of sql.
   *
   * @return jooq result-list
   */
  def fetchAllByCriterias(): Future[Result[Record3[Integer, Integer, String]]] = db.withTransaction { dsl =>
    val x = AUTHOR as "x"
    dsl.select(
      BOOK.ID * BOOK.AUTHOR_ID,
      BOOK.ID + BOOK.AUTHOR_ID * 3 + 4,
      BOOK.TITLE || " abc" || " xy"
    )
      .from(BOOK)
      .leftOuterJoin(
        select(x.ID, x.YEAR_OF_BIRTH)
          from x
          limit 1
          asTable x.getName()
      )
      .on(BOOK.AUTHOR_ID === x.ID)
      .where(BOOK.ID <> 2)
      .or(BOOK.TITLE in("O Alquimista", "Brida"))
      .fetch
  }


  /**
   * Fetches all books that belong to the given book-store names
   *
   * @return list of books
   */
  def fetchAllByBookStoreNames(names: Seq[String]): Future[Seq[BookRecord]] = db.query { dsl =>
    dsl
      .select(BOOK.fields.toList.asJava)
      .from(BOOK)
      .join(BOOK_TO_BOOK_STORE).on(BOOK_TO_BOOK_STORE.BOOK_ID.eq(BOOK.ID))
      .join(BOOK_STORE).on(BOOK_STORE.NAME.eq(BOOK_TO_BOOK_STORE.NAME))
      .where(BOOK_STORE.NAME.in(names.asJava))
      .fetch().asScala
      .map({
        case (record) =>
          // because of the join-fetch we must resolve our book-record from the abstract result,
          // that possibly contains data from multiple (joined) tables.
          record.into(BOOK).into(classOf[BookRecord])
      })
  }


}
