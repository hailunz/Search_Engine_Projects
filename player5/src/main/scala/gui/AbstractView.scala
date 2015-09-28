import module.SongModel

import scalafx.scene.Node

/**
 * Created by yangwu on 4/4/15.
 */
abstract class AbstractView(protected val songMadel: SongModel) {
  private val _viewNode: Node = initView()

  def viewNode: Node = _viewNode

  protected def initView(): Node
}
