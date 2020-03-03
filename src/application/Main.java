package application;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Screen;
import javafx.stage.Stage;
import netscape.javascript.JSObject;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;

public class Main extends Application {
  private JavaConnector javaConnector = new JavaConnector(config.username, config.credential, config.databaseName);

  public static void main(String[] args) {
    launch(args);
  }

  public void start(Stage primaryStage) {
    StackPane root = new StackPane();

    WebView webView = new WebView();
    File html = new File("html/index.html");
    webView.setContextMenuEnabled(false);
    WebEngine webEngine = webView.getEngine();
    webEngine.load(html.toURI().toString());

    webEngine.getLoadWorker().stateProperty().addListener(
            new ChangeListener() {
              @Override
              public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                if (newValue != Worker.State.SUCCEEDED) {
                  return;
                }

                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("javaConnector", javaConnector);
              }
            }
    );

    root.getChildren().add(webView);

    Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
    Scene scene = new Scene(root, visualBounds.getWidth(), visualBounds.getHeight());

    File logo = new File("html/images/logo.png");
    primaryStage.getIcons().add(new Image(logo.toURI().toString()));
    primaryStage.setTitle("Sportiza");
    primaryStage.setScene(scene);
    primaryStage.setMaximized(true);
    primaryStage.show();
  }

  public class JavaConnector {
    Connection conn;

    JavaConnector(String username, String credential, String database) {
      try {
        Class.forName("org.postgresql.Driver");
        conn = DriverManager.getConnection(database, username, credential);
        System.out.println("Connected to sportizadevspace database!");
      } catch (Exception e) {
        System.out.println(e);
      }
    }

    //request
    public void playerFormRequest(String FirstName, String LastName, String Team, String UniformNumber, String HomeTown) {
      //formating strings inputs for SQL command
      if (!FirstName.equals("NULL")) {
        FirstName = FirstName.replace("'", "''");
        FirstName = String.format("'%s'", FirstName);
      }
      if (!LastName.equals("NULL")) {
        LastName = LastName.replace("'", "''");
        LastName = String.format("'%s'", LastName);
      }
      if (!Team.equals("NULL")) {
        Team = Team.replace("'", "''");
        Team = String.format("'%s'", Team);
      }
      if (!UniformNumber.equals("NULL")) {
        UniformNumber = UniformNumber.replace("'", "''");
        UniformNumber = String.format("'%s'", UniformNumber);
      }
      if (!HomeTown.equals("NULL")) {
        HomeTown = HomeTown.replace("'", "''");
        HomeTown = String.format("'%s'", HomeTown);
      }
      //base query format
      String query = "SELECT DISTINCT ON (playerChosen.\"id\") playerChosen.\"id\", \"First Name\",\"Last Name\",\"Home Town\",\"Home Country\",\"Home State\",\"Position\", \"name\" FROM  (SELECT DISTINCT ON (\"players\".\"id\")  \"players\".\"id\",\"players\".\"First Name\", \"players\".\"Last Name\",\"players\".\"Position\", \"players\".\"Home Town\", \"players\".\"Home State\", \"players\".\"Home Country\",\"players\".\"Team Code\"\n" +
              "    FROM players\n" +
              "        WHERE \"First Name\" =  COALESCE(%2$s,\"players\".\"First Name\")\n" +
              "               and \"Last Name\" = COALESCE(%3$s,\"players\".\"Last Name\")\n" +
              "               and (\"players\".\"Uniform Number\" = COALESCE(%4$s, \"players\".\"Uniform Number\"))\n" +
              "               and (\"players\".\"Home Town\" = COALESCE(%5$s, \"players\".\"Home Town\")))as playerChosen\n" +
              "        INNER JOIN \"teams\"\n" +
              "            ON \"teams\".\"id\" = playerChosen.\"Team Code\" WHERE \"teams\".\"name\" = COALESCE(%1$s, \"teams\".\"name\");\n";
      if (HomeTown.equals("NULL")) {
        query = "SELECT DISTINCT ON (playerChosen.\"id\") playerChosen.\"id\", \"First Name\",\"Last Name\",\"Home Town\",\"Home Country\",\"Home State\",\"Position\", \"name\" FROM  (SELECT DISTINCT ON (\"players\".\"id\")  \"players\".\"id\",\"players\".\"First Name\", \"players\".\"Last Name\",\"players\".\"Position\", \"players\".\"Home Town\", \"players\".\"Home State\", \"players\".\"Home Country\",\"players\".\"Team Code\"\n" +
                "    FROM players\n" +
                "        WHERE \"First Name\" =  COALESCE(%2$s,\"players\".\"First Name\")\n" +
                "               and \"Last Name\" = COALESCE(%3$s,\"players\".\"Last Name\")\n" +
                "               and (\"players\".\"Uniform Number\" = COALESCE(%4$s, \"players\".\"Uniform Number\") )\n" +
                "               and (\"players\".\"Home Town\" = COALESCE(%5$s, \"players\".\"Home Town\") OR \"players\".\"Home Town\" IS NULL))as playerChosen\n" +
                "        INNER JOIN \"teams\"\n" +
                "            ON \"teams\".\"id\" = playerChosen.\"Team Code\" WHERE \"teams\".\"name\" = COALESCE(%1$s, \"teams\".\"name\");\n";
      }
      //loading values to empty query
      query = String.format(query, Team, FirstName, LastName, UniformNumber, HomeTown);
      //response for executed Query
      System.out.println(query);
      ResultSet response = this.executeQuery(query);
      try {
        FileWriter jsonFile = new FileWriter(config.requestPlayerFormFile);
        JSONArray fileObject = new JSONArray();
        while (response.next()) {
          String playerID = response.getString("id");
          String playerName = response.getString("First Name");
          if (response.wasNull()) {
            playerName = "N/A";
          }
          String playerLastName = response.getString("Last Name");
          if (response.wasNull()) {
            playerLastName = "N/A";
          }
          String playerTown = response.getString("Home Town");
          if (response.wasNull()) {
            playerTown = "N/A";
          }
          String position = response.getString("Position");
          if (response.wasNull()) {
            position = "N/A";
          }
          String playerState = response.getString("Home State");
          if (response.wasNull()) {
            playerState = "N/A";
          }
          String playerCountry = response.getString("Home Country");
          if (response.wasNull()) {
            playerCountry = "N/A";
          }
          //System.out.println("First name: " + playerName + ", Last Name: " + playerLastName+ ", Position: " + position +  ", Home Town: " + playerTown + ", State: " + playerState + ", Country: " + playerCountry);
          JSONObject userObject = new JSONObject();
          userObject.put("id", playerID);
          userObject.put("First Name", playerName);
          userObject.put("Last Name", playerLastName);
          userObject.put("Home Town", playerTown);
          userObject.put("Home State", playerState);
          userObject.put("Home Country", playerCountry);
          userObject.put("Position", position);
          fileObject.put(userObject);
        }
        jsonFile.write("var query = ");
        jsonFile.write(fileObject.toString());
        jsonFile.write(";");
        jsonFile.close();
        System.out.println("Request with Parameters: First Name: " + FirstName + ", Last Name: " + LastName + ", " + "Team: " + Team + ", Uniform Number: " + UniformNumber + ", Home Town: " + HomeTown + "------completed");
      } catch (SQLException | JSONException | IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }

    public ResultSet executeQuery(String query) {
      ResultSet response = null;
      try {
        Statement st = conn.createStatement();
        response = st.executeQuery(query);
      } catch (SQLException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      return response;

    }

  }
}