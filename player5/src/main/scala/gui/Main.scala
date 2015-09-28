import javafx.event.EventHandler
import javafx.stage.WindowEvent

import com.sun.javafx.runtime.VersionInfo

import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.scene.Scene

object Main extends JFXApp {

  println("JavaFX version: " + VersionInfo.getRuntimeVersion)

  val menu = new MenuView()

  var playList = new PlayListView(null)

  stage = new PrimaryStage {
    title = "Echo - Connecting Your Music"
    scene = new Scene(menu.pane, 500, 300) {
      val stylesheet = getClass.getResource("media.css")
      stylesheets.add(stylesheet.toString)
    }
    onCloseRequest = new EventHandler[WindowEvent]() {
      override def handle(ev: WindowEvent) = {
        try {
          playList.shutDown()
          println("- CLOSING -")
        } catch {
          case _ => stage.close()
        }
      }
    }
  }

}