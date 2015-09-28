/**
 * Created by yangwu on 4/10/15.
 */

package sample.cluster.simple


sealed trait RemoteMessage
case class reject() extends RemoteMessage {

}
case class agree(src:String) extends RemoteMessage{

}

case class permitCS() extends RemoteMessage
case class rejectCS() extends RemoteMessage
case class receiveOver() extends RemoteMessage