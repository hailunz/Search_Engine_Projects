/**
 * Created by yangwu on 4/9/15.
 */

sealed trait LocalMessage
case class Send(ip: String, port: Int, mesg: String) extends LocalMessage
case class Join(ip: String, port: String, mesg: String) extends LocalMessage
case class Leave() extends LocalMessage
case class requestPlay(src:String) extends LocalMessage

// interact with UI for vote
case class localAgree() extends LocalMessage
case class localReject() extends LocalMessage

