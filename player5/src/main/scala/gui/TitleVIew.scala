import module.SongModel

import scalafx.Includes._
import scalafx.scene.Node
import scalafx.scene.effect.DropShadow
import scalafx.scene.layout.{HBox, Priority, Region, VBox}
import scalafx.scene.paint.Color._
import scalafx.scene.paint.{LinearGradient, Stops}
import scalafx.scene.text.{Font, FontWeight, Text}

/**
 * Created by yangwu on 4/4/15.
 */

class TitleView(songModel: SongModel) extends AbstractView(songModel) {

  def initView(): Node = {
    new VBox {
      vgrow = Priority.ALWAYS
      hgrow = Priority.ALWAYS

      children = new HBox() {
        prefHeight = 76
        maxHeight = 76
        id = "mainToolBar"
        content = List(
          new Region {
            minWidth = 30
          },
          new Text {
            text = "Echo"
            font = Font.font("SansSerif", FontWeight.BOLD, 20)
            style = "-fx-font-size: 50pt"
            fill = new LinearGradient(
              endX = 0,
              stops = Stops(Cyan, DodgerBlue)
            )
            effect = new DropShadow {
              color = DodgerBlue
              radius = 25
              spread = 0.25
            }
          }
        )
      }

    }
  }

}