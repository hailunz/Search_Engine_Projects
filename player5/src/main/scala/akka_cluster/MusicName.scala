/**
 * Created by yangwu on 4/11/15.
 */

object MusicName {
  var _name = ""

  // getter
  def name = _name

  // setter
  def name_= (value:String):Unit = _name = value
}