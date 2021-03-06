package controllers

import database.DB
import dto.models.{AuthorModel, BooksWithAuthorsModel}
import dto.{AuthorDTO, BookDTO}
import generated.Tables._
import generated.tables.records.AuthorRecord
import javax.inject.{Inject, Singleton}
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import play.twirl.api.Html

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class TestController @Inject()(
                                cc: ControllerComponents,
                                db: DB,
                                bookDTO: BookDTO,
                                authorDTO: AuthorDTO) extends AbstractController(cc) with I18nSupport {

    /**
     * Executes a complex fetch with a join.
     *
     * @return result
     */
    def test1: Action[AnyContent] = Action.async { implicit request =>
        val result = for {
            fetchResult <- bookDTO.fetchAllByCriterias()
        } yield (fetchResult)

        result.map({
            list =>
                // render html-view (transform the fetched jOOQ-Records to HTML)
                Ok(views.html.test1(Html(list.formatHTML())))
        })

    }


    /**
     * Inserts 5000 entries with a batch-insert
     *
     * @return success-status
     */
    def test2: Action[AnyContent] = Action.async { implicit request =>

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
            fetchResult <- authorDTO.insertMany(authors)
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
    def test3: Action[AnyContent] = Action.async { implicit request =>
        // delete authors with batch-query
        val result = for {
            fetchResult <- authorDTO.deleteManyLikeFirstName("%Max%")
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
    def test4: Action[AnyContent] = Action.async { implicit request =>

        db.withTransaction({ dsl =>
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
    def test5: Action[AnyContent] = Action.async { implicit request =>

        db.withTransaction({ dsl =>
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
     * Tests multi-joined fetch
     *
     * @return success-status
     */
    def test6: Action[AnyContent] = Action.async { implicit request =>

        val result = for {
            // those statements are potentially in parallel with two parallel running connections,
            // but because the second depends on the first, the second is waiting until the first is finished.
            books <- bookDTO.fetchAllByBookStoreNames(Seq("Orell Füssli"))
            authors <- authorDTO.fetchAllByBookIds(books.map(x => x.getId))
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
                              author.getId, Some(author.getFirstName), author.getLastName, Some(author.getDateOfBirth),
                              Some(author.getYearOfBirth), Some(author.getDistinguished)));
                        new BooksWithAuthorsModel(
                            id = book.getId,
                            authors = foundAuthors);
                })
                Ok(Json.obj(
                    "booksWithAuthors" -> booksWithAuthors
                ))
        })
    }
}