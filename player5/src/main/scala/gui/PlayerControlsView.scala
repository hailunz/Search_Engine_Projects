import java.io.File

import module.SongModel

import scalafx.Includes._
import scalafx.application.Platform
import scalafx.event.ActionEvent
import scalafx.event.subscriptions.Subscription
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.Node
import scalafx.scene.control.{Button, Label}
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.layout.{BorderPane, GridPane, HBox, StackPane}
import scalafx.scene.media.MediaPlayer
import scalafx.scene.media.MediaPlayer.Status
import scalafx.scene.paint.Color
import scalafx.scene.shape.Rectangle
import scalafx.stage.Popup

/**
 * Created by yangwu on 4/4/15.
 */
class PlayerControlsView(songModel: SongModel, ip:String, port:String, config:ClusterConfig) extends AbstractView(songModel) {
  private var pauseImg: Image = _
  private var playImg: Image = _
  private var playPauseIcon: ImageView = _
  /** 'subscription' keep track of assigned listeners */
  private var statusInvalidationSubscription: Subscription = _
  private var currentTimeSubscription: Subscription = _
  private var controlPanel: Node = _


  songModel.mediaPlayer.onChange(
    (_, oldValue, newValue) => {
      if (oldValue != null)
          removeListenersAndBinding(oldValue)
      addListenersAndBindings(newValue)
    }
  )

  addListenersAndBindings(songModel.mediaPlayer())

  protected def initView(): Node = {
//    val likeButton = createLikeButton()
//    val hateButton = createHateButton()

    controlPanel = createControlPanel()

    val gp = new GridPane {
      hgap = 1
      vgap = 1
      padding = Insets(10)
    }
    // columnIndex: Int, rowIndex: Int, colspan: Int, rowspan: Int
    gp.add(controlPanel, 4, 0, 1, 2)
//    gp.add(likeButton, 5, 0, 1, 2)
//    gp.add(hateButton, 6, 0, 1, 2)

    gp
  }

  private def createLikeButton() = new Button {
    id = "likeButton"
    graphic = new ImageView {
      image = new Image(this.getClass.getResourceAsStream("/up.png"), 30, 30, false, true)
    }
    // TODO: spread out votes
    onAction = (ae: ActionEvent) => {
      println("- CAPTURE LIKE -")
    }
  }


  private def createControlPanel(): Node = {
    val playPauseButton = createPlayPauseButton()


    new HBox {
      alignment = Pos.CENTER
      fillHeight = false
      content =  playPauseButton
    }
  }


  private def createPlayPauseButton(): Button = {
    val pauseUrl = getClass.getResource("/pause.png")
    pauseImg = new Image(pauseUrl.toString)

    val playUrl = getClass.getResource("/play.png")
    playImg = new Image(playUrl.toString)

    playPauseIcon = new ImageView {
      image = playImg
    }

    new Button {
      graphic = playPauseIcon
      id = "playPauseButton"
      onAction = (ae: ActionEvent) => {
        val mediaPlayer = songModel.mediaPlayer()
        mediaPlayer.status() match {
          case Status.PLAYING.delegate => mediaPlayer.pause()
          case _ =>
            println(" - BROADCASTING: " + MusicName.name)
            config.broadcast(MusicName.name)  /** where it starts **/
        }
      }
    }

  }

  private def updateStatus(newStatus: Status) {
    if (Status.UNKNOWN == newStatus || newStatus == null) {
      controlPanel.disable = true
    } else {
      controlPanel.disable = false
      playPauseIcon.image = if (Status.PLAYING == newStatus) pauseImg else playImg
    }
  }


  private def addListenersAndBindings(mp: MediaPlayer) {
    statusInvalidationSubscription = mp.status.onInvalidate {
      Platform.runLater {
        updateStatus(songModel.mediaPlayer().status())
      }
    }

    mp.onEndOfMedia = songModel.mediaPlayer().stop()

  }

  private def removeListenersAndBinding(mp: MediaPlayer) {
    statusInvalidationSubscription.cancel()
  }

  def playMusic(music:String): Unit = {
    println("checkpoint")
    val url:String = new File("./"+music+".mp3").toURI().toString()
    println("URL: "+url)
    songModel.url = url
    songModel.mediaPlayer().play()
  }

}
