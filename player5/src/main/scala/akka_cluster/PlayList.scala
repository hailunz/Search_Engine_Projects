import java.io.File

import module.SongModel

import scala.collection._
import scalafx.application.Platform
import scalafx.scene.media.MediaPlayer
import scalafx.scene.media.MediaPlayer.Status

/**
 * Created by yangwu on 4/10/15.
 */

case class PlayList() {

  var arr = mutable.Buffer("Rolling In The Deep", "Alex Goot - Counting Stars", "Radioactive")

  def select(name:String): Unit = {
    println("- PLAYLIST SELECT -"+name)
    val idx = arr.indexOf(name)
    println("IDX = "+idx)
    if (idx>0) {
      val tmp = arr(0)
      arr.update(0, name)
      arr.update(idx, tmp)
    }
    println("- UPDATED LIST: "+arr(0)+"; "+arr(1)+"; "+arr(2))
    /** set music name **/
    MusicName.name = arr(0)
    println(MusicName.name)
  }

}