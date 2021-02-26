package ParameterCalculation.helper.dbhandler;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.sqlite.SQLiteConfig;

public class DbHandler {

  public static Connection getMAConnection() throws SQLException, ClassNotFoundException {
    SQLiteConfig config = new SQLiteConfig();
    config.enableLoadExtension(true);
    Class.forName("org.sqlite.JDBC");
    return DriverManager.getConnection(
        "jdbc:sqlite:MastersThesisDatabaseSchott.sqlite",
        config.toProperties());
  }

  public static Connection getAltMAConnection() throws SQLException, ClassNotFoundException {
    SQLiteConfig config = new SQLiteConfig();
    config.enableLoadExtension(true);
    Class.forName("org.sqlite.JDBC");
    return DriverManager.getConnection(
        "jdbc:sqlite:MastersThesisDatabaseSchottAlt.sqlite",
        config.toProperties());
  }

  public static Connection getKeytables() throws SQLException, ClassNotFoundException {
    //Class.forName("org.postgresql.Driver");
    return DriverManager.getConnection(
        "jdbc:postgresql://OSHDBKeytables", "User", "PW");
  }

  public static Connection getLocalKeytables() throws SQLException, ClassNotFoundException {
    //Class.forName("org.postgresql.Driver");
    return DriverManager.getConnection(
        "jdbc:h2:/LocalKeytables", "User", "PW");
  }

  public static Connection getChangesets() throws SQLException, ClassNotFoundException {
    Class.forName("org.postgresql.Driver");
    return DriverManager.getConnection(
        "jdbc:postgresql://ChangesetDB", "User", "PW");
  }

  public static Connection getNotes() throws ClassNotFoundException, SQLException {
    Class.forName("org.postgresql.Driver");
    return DriverManager.getConnection(
        "jdbc:postgresql://NotesDB", "User", "PW");
  }

}
