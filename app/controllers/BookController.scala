package controllers

import dto.BookDTO
import generated.tables.records.BookRecord
import javax.inject.{Inject, Singleton}
import play.api.i18n.I18nSupport
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import play.twirl.api.Html

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class BookController @Inject()(
                                cc: ControllerComponents,
                                bookDTO: BookDTO) extends AbstractController(cc) with I18nSupport {

    /**
     * fetch all books and show them as view
     *
     * @return books-view
     */
    def fetchAllBooks: Action[AnyContent] = Action.async { implicit request =>

        // fetch all books from the database
        val result = for {
            all <- bookDTO.fetchAll()
        } yield all

        result.map({
            books =>
                // render html-view (transform the fetched jOOQ-Records to HTML)
                Ok(views.html.fetchAllBooks(Html(books.formatHTML()), BookAddForm.form, BookDeleteForm.form))
        })
    }

    /**
     * add a new book to the list
     *
     * @return book-data as json
     */
    def addBook: Action[AnyContent] = Action.async { implicit request =>

        val formData: BookAddForm = BookAddForm.form.bindFromRequest.get

        // put the form-data into a jOOQ-Record.
        val bookRecord: BookRecord = new BookRecord();
        bookRecord.setAuthorId(formData.author_id);
        bookRecord.setTitle(formData.title);
        bookRecord.setPublishedIn(formData.published_in);
        bookRecord.setLanguageId(formData.language_id);

        // insert the jOOQ-Record to the database.
        bookDTO.insertOne(bookRecord) map ({
            insertRecord =>
                // show inserted record as json
                Ok(insertRecord.formatJSON());
        });
    }

    /**
     * delete a book from the list
     *
     * @return success-status
     */
    def deleteBook: Action[AnyContent] = Action.async { implicit request =>

        val formData: BookDeleteForm = BookDeleteForm.form.bindFromRequest.get

        val bookRecord: BookRecord = new BookRecord();
        bookRecord.setId(formData.id);

        bookDTO.deleteOne(bookRecord) map ({
            deleteStatus =>
                // show delete status
                Ok("OK: " + deleteStatus);
        });
    }
}
