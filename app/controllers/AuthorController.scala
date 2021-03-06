package controllers

import database.DB
import dto.AuthorDTO
import generated.tables.records.AuthorRecord
import javax.inject.{Inject, Singleton}
import play.api.i18n.I18nSupport
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import play.twirl.api.Html

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class AuthorController @Inject()(
                                  cc: ControllerComponents,
                                  db: DB,
                                  authorDTO: AuthorDTO) extends AbstractController(cc) with I18nSupport {

    /**
     * fetch all authors and show them as view
     *
     * @return authors-view
     */
    def fetchAllAuthors: Action[AnyContent] = Action.async { implicit request =>

        val t0 = System.currentTimeMillis()

        // fetch all authors from the database
        val result = for {
            all <- authorDTO.fetchAll()
        } yield all

        result.map({
            authors =>
                // render html-view (transform the fetched jOOQ-Records to HTML)
                val t1 = System.currentTimeMillis()
                println("Elapsed time: " + (t1 - t0) + "ms")

                // memory info
                val mb = 1024 * 1024
                val runtime = Runtime.getRuntime
                println("ALL RESULTS IN MB")
                println("** Used Memory:  " + (runtime.totalMemory - runtime.freeMemory) / mb)
                println("** Free Memory:  " + runtime.freeMemory / mb)
                println("** Total Memory: " + runtime.totalMemory / mb)
                println("** Max Memory:   " + runtime.maxMemory / mb)


                Ok(views.html.fetchAllAuthors(Html(authors.formatHTML()), AuthorAddForm.form,
                    AuthorDeleteForm.form))
        })
    }

    /**
     * add a new author to the list
     *
     * @return author-data as json
     */
    def addAuthor: Action[AnyContent] = Action.async { implicit request =>

        val formData: AuthorAddForm = AuthorAddForm.form.bindFromRequest.get

        // put the form-data into a jOOQ-Record.
        val authorRecord: AuthorRecord = new AuthorRecord();
        authorRecord.setFirstName(formData.first_name);
        authorRecord.setLastName(formData.last_name);

        // insert the jOOQ-Record to the database.
        authorDTO.insertOne(authorRecord) map ({
            insertRecord =>
                // show inserted record as json
                Ok(insertRecord.formatJSON());
        });
    }

    /**
     * delete a author from the list
     *
     * @return success-status
     */
    def deleteAuthor: Action[AnyContent] = Action.async { implicit request =>

        val formData: AuthorDeleteForm = AuthorDeleteForm.form.bindFromRequest.get

        val authorRecord: AuthorRecord = new AuthorRecord();
        authorRecord.setId(formData.id);

        authorDTO.deleteOne(authorRecord) map ({
            deleteStatus =>
                // show delete status
                Ok("OK: " + deleteStatus);
        });
    }
}
