import java.io.File
import java.net.InetAddress

import module.SongModel

import scalafx.scene.control.Label
import scalafx.scene.layout.{VBox, BorderPane, Priority, HBox}

/**
 * Created by yangwu on 4/23/15.
 */

class PrimaryView(ip:String, port:String) {
  private final val songModel = new SongModel() {
    url = new File("./Rolling In The Deep.mp3").toURI().toString()
    print("URL: ")
    println(url)
  }

  SongModel.url = new File("./Rolling In The Deep.mp3").toURI().toString()

  var config = new ClusterConfig(ip, port)

  private val titleView = new TitleView(songModel)
  private val metaDataView = new MetadataView(songModel)

  val playerControlsView = new PlayerControlsView(songModel, ip, port, config)

  private val hostIP: String = "128.237.176.210"
  private val localIP: String = InetAddress.getLocalHost.getHostAddress


  val _root = new HBox {
    vgrow = Priority.ALWAYS
    hgrow = Priority.ALWAYS
    children = List (
      new BorderPane {
        top = new VBox() {
          content = titleView.viewNode
        }
        center = new VBox() {
          children =
            metaDataView.viewNode
        }
        bottom = new VBox() {
          children = List(playerControlsView.viewNode,
            new Label(" Host IP: " + hostIP + "  Local IP: " + localIP) {
              id = "ip"
            }
          )
        }
        maxWidth = 350
      },
      new VBox() {
        var playList = new PlayListView(config)
        Main.playList = playList
        children = playList.initView()
      }
    )
  }

  def root = _root
}