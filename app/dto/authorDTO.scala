package dto

import database.DB
import generated.Tables._
import generated.tables.records.{AuthorRecord, BookRecord}
import javax.inject.{Inject, Singleton}
import org.jooq.impl.DSL.select
import org.jooq.scalaextensions.Conversions._
import org.jooq.{DSLContext, Record3, Result}

import scala.collection.JavaConverters._
import scala.concurrent.Future

@Singleton
class authorDTO @Inject()(db: DB) {


  /**
   * returns a list of author-records
   *
   * @return list
   */
  def fetchAll(): Future[Result[AuthorRecord]] = db.withTransaction { dsl =>
    dsl
      .selectFrom(AUTHOR)
      .fetch();
  }

  /**
   * returns one author-record by its id
   *
   * @param id
   * @return author
   */
  def fetchOneById(id: Int): Future[AuthorRecord] = db.withTransaction { dsl =>
    dsl
      .selectFrom(AUTHOR)
      .where(AUTHOR.ID.eq(id))
      .fetchOne();
  }

  /**
   * returns one author-record by its id
   *
   * @param id
   * @return author
   */
  def fetchOneById(dsl:DSLContext, id: Int): AuthorRecord = {
    dsl
      .selectFrom(AUTHOR)
      .where(AUTHOR.ID.eq(id))
      .fetchOne();
  }


  /**
   * insert one author-record
   *
   * @param author
   * @return author with auto-generated id
   */
  def insertOne(author: AuthorRecord): Future[AuthorRecord] = db.withTransaction { dsl =>
    dsl
      .insertInto(AUTHOR)
      .set(author)
      .returning(AUTHOR.ID)
      .fetchOne()
      .map({
        result =>
          author.setId(result.get(AUTHOR.ID));
          author;
      })
  }

  /**
   * insert author-records
   *
   * @param authors
   * @return insert-counts
   */
  def insertMany(authors: Seq[AuthorRecord]): Future[Array[Int]] = db.withTransaction { dsl =>
    dsl
      .batchInsert(authors.asJava)
      .execute()
  }

  /**
   * delete author-records
   *
   * @param authors
   * @return delete-counts
   */
  def deleteMany(authors: Seq[AuthorRecord]): Future[Int] = db.withTransaction { dsl =>
    dsl
      .delete(AUTHOR).where(AUTHOR.ID.in(authors.map(x => x.getId).asJava))
      .execute()
  }


  /**
   * delete author-records that contain a first_name that starts with the given
   *
   * @param first_nameLike
   * @return delete-counts
   */
  def deleteManyLikeFirstName(first_nameLike: String): Future[Int] = db.withTransaction { dsl =>
    dsl
      .delete(AUTHOR).where(AUTHOR.FIRST_NAME.like(first_nameLike))
      .execute()
  }

  /**
   * update one author-record by its id
   *
   * @param author
   * @return author
   */
  def updateOne(author: AuthorRecord): Future[AuthorRecord] = db.withTransaction { dsl =>
    dsl
      .update(AUTHOR)
      .set(author)
      .where(AUTHOR.ID.eq(author.getId))
      .returning(AUTHOR.ID)
      .fetchOne();
  }

  /**
   * delete one author-record by its id
   *
   * @param author
   * @return delete-count (1)
   */
  def deleteOne(author: AuthorRecord): Future[Int] = db.withTransaction { dsl =>
    dsl
      .delete(AUTHOR)
      .where(AUTHOR.ID.eq(author.getId))
      .execute()
  }


  /**
   * upsert a author (meaning: inserting it if it does not exist, updating it if exists already)
   *
   * @param author
   * @return upserted author
   */
  def upsertOne(author: AuthorRecord): Future[Int] = db.withTransaction { dsl =>
    dsl.insertInto(AUTHOR)
      .set(author)
      .onDuplicateKeyUpdate()
      .set(author)
      .execute();
  }



}
