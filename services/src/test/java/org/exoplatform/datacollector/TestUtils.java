package org.exoplatform.datacollector;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.exoplatform.datacollector.domain.RelevanceEntity;
import org.exoplatform.datacollector.domain.RelevanceId;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;

public class TestUtils {

	private static Connection conn;
	private static Liquibase liquibase;
	public static RelevanceId EXISTING_RELEVANCE_ID = new RelevanceId("1", "2");
	public static RelevanceId UNEXISTING_RELEVANCE_ID = new RelevanceId("1", "3");

	public static void initH2DB() throws SQLException, ClassNotFoundException, LiquibaseException {

		Class.forName("org.h2.Driver");
		conn = DriverManager.getConnection("jdbc:h2:target/h2-db", "sa", "");

		initDB();
	}

	public static void initHSQLDB() throws LiquibaseException, SQLException, ClassNotFoundException {

		Class.forName("org.hsqldb.jdbcDriver");
		conn = DriverManager.getConnection("jdbc:hsqldb:file:target/hsql-db", "sa", "");

		initDB();
	}

	private static void initDB() throws LiquibaseException {
		Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(conn));

		liquibase = new Liquibase("db/changelog/datacollector.db.changelog-1.0.0.xml", new ClassLoaderResourceAccessor(),
				database);
		liquibase.update((String) null);
		
	}

	public static void closeDB() throws LiquibaseException, SQLException {
		liquibase.rollback(1000, null);
		conn.close();
	}

	public static RelevanceEntity getExistingRelevance() {
		RelevanceEntity relevance = new RelevanceEntity();
		relevance.setUserId("1");
		relevance.setActivityId("2");
		relevance.setRelevant(true);
		return relevance;
	}

	public static RelevanceEntity getNewRelevance() {
		RelevanceEntity relevance = new RelevanceEntity();
		relevance.setUserId("1");
		relevance.setActivityId("3");
		relevance.setRelevant(true);
		return relevance;
	}
}
