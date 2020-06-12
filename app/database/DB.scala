package database

import javax.inject.Inject

import akka.actor.ActorSystem
import org.jooq.{SQLDialect, DSLContext}
import org.jooq.impl.DSL
import play.api.db.Database

import scala.concurrent.{ExecutionContext, Future}

class DB @Inject() (db: Database, system: ActorSystem) {

  val databaseContext: ExecutionContext = system.dispatchers.lookup("contexts.database")

  def query[A](block: DSLContext => A): Future[A] = Future {
    db.withConnection { connection =>
      val dsl = DSL.using(connection, SQLDialect.MARIADB)
      block(dsl)
    }
  }(databaseContext)

  def withTransaction[A](block: DSLContext => A): Future[A] = Future {
    db.withTransaction { connection =>
      val dsl = DSL.using(connection, SQLDialect.MARIADB)
      block(dsl)
    }
  }(databaseContext)

}
