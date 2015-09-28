import module.SongModel

import scalafx.Includes._
import scalafx.geometry.{Insets, VPos}
import scalafx.scene.Node
import scalafx.scene.control.Label
import scalafx.scene.effect.Reflection
import scalafx.scene.image.ImageView
import scalafx.scene.layout.{ColumnConstraints, GridPane, Priority, RowConstraints}


/**
 * Created by yangwu on 4/4/15.
 */
class MetadataView(songModel: SongModel) extends AbstractView(songModel) {

  def initView(): Node = {
    val title = new Label {
      text <== SongModel.title
      id = "title"
    }
    val artist = new Label {
      text <== SongModel.artist
      id = "artist"
    }
    val album = new Label {
      text <== SongModel.album
      id = "album"
    }
    val year = new Label {
      text <== SongModel.year
      id = "year"
    }
    val albumCover = new ImageView {
      image <== SongModel.albumCover
      fitWidth = 95
      preserveRatio = true
      smooth = true
      effect = new Reflection {
        fraction = 0.2
      }
    }

    new GridPane {
      padding = Insets(10)
      hgap = 20
      add(albumCover, 0, 0, 1, GridPane.REMAINING)
      add(title, 1, 0)
      add(artist, 1, 1)
      add(album, 1, 2)
      add(year, 1, 3)
      columnConstraints += (
        new ColumnConstraints(),
        new ColumnConstraints {
          hgrow = Priority.ALWAYS
        }.delegate
        )
      val r0 = new RowConstraints {
        valignment = VPos.TOP
      }
      rowConstraints += (r0, r0, r0, r0)
    }

  }
}
