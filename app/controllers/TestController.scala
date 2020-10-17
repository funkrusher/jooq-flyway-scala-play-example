package controllers

import dto.{AuthorRepository, BookRepository, jooqAuthorDTO, jooqBookDTO}
import generated.tables.records.AuthorRecord
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import play.twirl.api.Html
import database.DB
import generated.Tables._
import generated.tables.records.{AuthorRecord, BookRecord}
import play.api.i18n.I18nSupport
import javax.inject.Inject
import javax.inject.Singleton
import database.DB
import dto.models.{AuthorModel, BookModel, BooksWithAuthorsModel}
import play.api.libs.json.Json
import play.api.libs.json.OFormat

import scala.concurrent.ExecutionContext.Implicits.global
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

@Singleton
class TestController @Inject()(
                                dbConfigProvider: DatabaseConfigProvider,
                                cc: ControllerComponents,
                                jooqDb: DB,
                                jooqBookDTO: jooqBookDTO,
                                jooqAuthorDTO: jooqAuthorDTO,
                                authorRepository: AuthorRepository,
                                bookRepository: BookRepository) extends AbstractController(cc) with I18nSupport {
  // We want the JdbcProfile for this provider
  val dbConfig = dbConfigProvider.get[JdbcProfile]

  // These imports are important, the first one brings db into scope, which will let you do the actual db operations.
  // The second one brings the Slick DSL into scope, which lets you define the table and other queries.

  import dbConfig._
  import profile.api._


  /**
   * Executes a complex fetch with a join.
   *
   * @return result
   */
  def jooqTest1: Action[AnyContent] = Action.async { implicit request =>
    val result = for {
      fetchResult <- jooqBookDTO.fetchAllByCriterias()
    } yield (fetchResult)

    result.map({
      list =>
        // render html-view (transform the fetched jOOQ-Records to HTML)
        Ok(views.html.jooqTest1(Html(list.formatHTML())))
    })

  }


  /**
   * Inserts 5000 entries with a batch-insert
   *
   * @return success-status
   */
  def jooqTest2: Action[AnyContent] = Action.async { implicit request =>

    // create 5000 authors
    var authors: Seq[AuthorRecord] = Seq();
    (1 to 5000).foreach(i => {
      val author: AuthorRecord = new AuthorRecord();
      author.setFirstName("Max" + i);
      author.setLastName("Mustermann" + i);
      authors = authors :+ author
    })

    // insert 5000 authors with batch-query
    val result = for {
      fetchResult <- jooqAuthorDTO.insertMany(authors)
    } yield (fetchResult)

    result.map({
      list =>
        // render html-view (transform the fetched jOOQ-Records to HTML)
        Ok("OK")
    })
  }

  /**
   * Deletes all authors that have a specific first_name pattern
   *
   * @return success-status
   */
  def jooqTest3: Action[AnyContent] = Action.async { implicit request =>
    // delete authors with batch-query
    val result = for {
      fetchResult <- jooqAuthorDTO.deleteManyLikeFirstName("%Max%")
    } yield (fetchResult)

    result.map({
      list =>
        // render html-view (transform the fetched jOOQ-Records to HTML)
        Ok("OK")
    })
  }

  /**
   * Tests transactional success
   *
   * @return success-status
   */
  def jooqTest4: Action[AnyContent] = Action.async { implicit request =>

    jooqDb.withTransaction({ dsl =>
      // we are within a database transaction now.

      var author = dsl.selectFrom(AUTHOR).where(AUTHOR.ID.eq(1)).fetchOne();
      author.setId(null);
      author.setFirstName("Peter");

      val c = dsl.insertInto(AUTHOR).set(author).returning(AUTHOR.ID).fetchOne().map({
        result =>
          author.setId(result.get(AUTHOR.ID));
          author;
      })

      // render html-view (transform the fetched jOOQ-Records to HTML)
      Ok("OK")
    })
  }


  /**
   * Tests transactional error with rollback
   *
   * @return success-status
   */
  def jooqTest5: Action[AnyContent] = Action.async { implicit request =>

    jooqDb.withTransaction({ dsl =>
      // we are within a database transaction now.

      var author = dsl.selectFrom(AUTHOR).where(AUTHOR.ID.eq(1)).fetchOne();
      author.setId(null);
      author.setFirstName("Carl");

      val c = dsl.insertInto(AUTHOR).set(author).returning(AUTHOR.ID).fetchOne().map({
        result =>
          author.setId(result.get(AUTHOR.ID));
          author;
      })

      var author2 = dsl.selectFrom(AUTHOR).where(AUTHOR.ID.eq(2)).fetchOne();
      author2.setId(null);
      author2.setFirstName("Linda");

      val c2 = dsl.insertInto(AUTHOR).set(author2).returning(AUTHOR.ID).fetchOne().map({
        result =>
          author.setId(result.get(AUTHOR.ID));
          author;
      })

      // transaction should be rolled-back: we can check this, after calling this route
      // the above inserts should not be done at all.
      throw new Exception("force a rollback of the transaction with this ex-throw!");

      // render html-view (transform the fetched jOOQ-Records to HTML)
      Ok("OK")
    })
  }

  /**
   * Tests transactional error with rollback
   *
   * @return success-status
   */
  def slickTest5: Action[AnyContent] = Action.async { implicit request =>

    // see also: https://github.com/slick/slick/issues/1197
    // see also: https://stackoverflow.com/questions/38221021/transactional-method-in-scala-play-with-slick-similar-to-spring-transactional

    val action = (for {
      // those statements are potentially in parallel with two parallel running connections,
      // but because the second depends on the first, the second is waiting until the first is finished.
      author <- authorRepository.fetchByIdAction(1)
      author2 <- authorRepository.fetchByIdAction(2)
      save1 <- authorRepository.saveAction(author.head.copy(id = 1000, first_name = Some("Gustave")))
      save2 <- authorRepository.saveAction(author.head.copy(id = 1001, first_name = Some("Lydia")))

    } yield (author, author2, save1, save2)).flatMap {
      case (author, author2, save1, save2) =>
        // lets force a rollback after all statements have been resolved
        // but while the transactionlly is still open.
        DBIO.failed(new Exception("force a rollback of the transaction with this ex-throw!"))
    }.transactionally;


    db.run(action).map({
      case (r) =>
        // render html-view
        Ok("OK")
    })
  }


  /**
   * Tests multi-joined fetch
   *
   * @return success-status
   */
  def jooqTest6: Action[AnyContent] = Action.async { implicit request =>

    val result = for {
      // those statements are potentially in parallel with two parallel running connections,
      // but because the second depends on the first, the second is waiting until the first is finished.
      books <- jooqBookDTO.fetchAllByBookStoreNames(Seq("Orell Füssli"))
      authors <- jooqAuthorDTO.fetchAllByBookIds(books.map(x => x.getId))
    } yield (books, authors)

    result.map({
      case (books, authors) =>
        // the books and authors are of the jOOQ-Record type.
        // we can not serialize them easily as json.
        // just to test it out we push the jOOQ-Record into Scala Case-Classes which can be serialized
        // but it would be a bad idea to reintroduce handwritten classes in addition to the autogenerated ones.
        // TODO maybe consider using Jackson-Serizalizer Library instead of the OFormat

        val booksWithAuthors: Seq[BooksWithAuthorsModel] = books.map({
          book =>
            val foundAuthors = authors
              .filter(author => author.getId.equals(book.getAuthorId))
              .map(author => new AuthorModel(
                author.getId, Some(author.getFirstName), author.getLastName, Some(author.getDateOfBirth), Some(author.getYearOfBirth), Some(author.getDistinguished)));
            new BooksWithAuthorsModel(
              id = book.getId,
              authors = foundAuthors);
        })
        Ok(Json.obj(
          "booksWithAuthors" -> booksWithAuthors
        ))
    })
  }

  /**
   * Tests multi-joined fetch
   *
   * @return success-status
   */
  def slickTest6: Action[AnyContent] = Action.async { implicit request =>

    val result = for {
      // those statements are potentially in parallel with two parallel running connections,
      // but because the second depends on the first, the second is waiting until the first is finished.
      books <- bookRepository.fetchAllByBookStoreNames(Seq("Orell Füssli"))
      authors <- authorRepository.fetchAllByBookIds(books.map(x => x.id))
    } yield (books, authors)

    result.map({
      case (books, authors) =>
        // the books and authors are of the jOOQ-Record type.
        // we can not serialize them easily as json.
        // just to test it out we push the jOOQ-Record into Scala Case-Classes which can be serialized
        // but it would be a bad idea to reintroduce handwritten classes in addition to the autogenerated ones.
        // TODO maybe consider using Jackson-Serizalizer Library instead of the OFormat

        val booksWithAuthors: Seq[BooksWithAuthorsModel] = books.map({
          book =>
            val foundAuthors = authors
              .filter(author => author.id.equals(book.author_id))
              .map(author => new AuthorModel(
                author.id, author.first_name, author.last_name, author.date_of_birth, author.year_of_birth, author.distinguished));
            new BooksWithAuthorsModel(
              id = book.id,
              authors = foundAuthors);
        })
        Ok(Json.obj(
          "booksWithAuthors" -> booksWithAuthors
        ))
    })
  }


}