import java.io.{FileOutputStream, FileInputStream, File}
import java.net.InetAddress
import java.util.Date

import akka.actor.{Actor, ActorLogging, ReceiveTimeout}
import akka.cluster.ClusterEvent._
import akka.cluster.{Cluster, MemberStatus, VectorClock}
import akka.contrib.pattern.{DistributedPubSubExtension, DistributedPubSubMediator}
import akka_cluster.FXUtils
import module._
import org.apache.commons.net.ntp.NTPUDPClient
import sample.cluster.simple._

import scala.collection.mutable.ArrayBuffer
import scala.concurrent._
import scala.concurrent.duration._
import scalafx.collections.ObservableBuffer
import scalax.io.{Output, Resource}
import ExecutionContext.Implicits.global
;

/**
 * Created by yangwu on 4/9/15.
 */

class SimpleClusterListener() extends Actor with ActorLogging {

  import DistributedPubSubMediator.{Publish, Subscribe, SubscribeAck}

  val cluster = Cluster(context.system)
  val members = cluster.state.members.filter(_.status == MemberStatus.Up);

  val mediator = DistributedPubSubExtension(context.system).mediator

  var currentSong : String = ""
  var countCS = 0
  var countAgree = 0
  var countReject = 0

  var currentSender = self
  var oriTime = 0L
  var hassend = false


  var state="released"

  // initialize vector clock
  var ts = new VectorClock()
  val name = self.hashCode().toString()

  // subscribe to the topic named "content"
  mediator ! Subscribe("content", self)

  var currentPlay = new Play

  var mem = new ObservableBuffer[String]()

  var inputFile : File =null
  var inputStream : FileInputStream=null
  var outputFile : File=null
  var outputStream : FileOutputStream=null

  var countRev =0

  // subscribe to cluster changes, re-subscribe when restart
  override def preStart(): Unit = {
    //#subscribe
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents,
      classOf[MemberEvent], classOf[UnreachableMember])
  }
  override def postStop(): Unit = cluster.unsubscribe(self)

  def receive = {
    /*
      Info about join and leave clusters
     */
    case MemberUp(member) =>
      log.info("Member is Up: {}", member.address)
      FXUtils.runAndWait {
        mem += member.address.host.toString.substring(4)
        Main.playList.updateMem(mem)
      }
      println("show members")
      for (a <- members){
        println(a.address)
      }

    case UnreachableMember(member) =>
      log.info("Member detected as unreachable: {}", member)

    case MemberRemoved(member, previousStatus) =>
      log.info("Member is Removed: {} after {}",
        member.address, previousStatus)

    /*
      voting mechanism
    */

    case x:Play =>
      println("- CHECK PLAY -" + x.musicName)
      sender ! agree(x.musicName)

    // requestPlay - ask for enter the critical section
    case requestPlay(src:String) =>
      println(" -   BROADCASTING  - RECEIVE: " + src)

      currentSong = src
      if(state=="released" ){
        state="wanted"
        ts=ts.:+(name)
        mediator ! Publish("content",new requestCS(ts))
      }else {
        println("request play failed:"+state)
      }


    //multicast request into critical section for play
    case x:requestCS =>
      println("requestCS")
      if (state=="released" || (state=="wanted" && x.t.<(ts))){
        state= "vote"
        println("- PERMIT CS -")
        sender ! permitCS()
      }else{
        println("- REJECT CS -")
        sender ! rejectCS()
      }

    case permitCS()=>
      println("permit CS")
      countCS+=1
      val size = cluster.state.members.filter(_.status == MemberStatus.Up).size
      if (countCS >= size && state!="hold"){
        state ="hold"
        println("hold")
        mediator ! Publish("content",new Vote(currentSong))
        context.setReceiveTimeout(10 seconds)
      }

    case rejectCS()=>
      println("reject CS")
      if (sender!=self){
        state="released"
        mediator ! Publish("content",new release())
        countCS = 0
      }else{
        countCS +=1
      }

    case release()=>
      // reset all value
      println("- RELEASE -")
      context.setReceiveTimeout(Duration.Undefined)
      hassend = false
      countAgree = 0
      countReject = 0
      countCS = 0
      countRev = 0
      state = "released"

    // someone has entered the CS, ask for voting
    case Vote(song:String) =>
      println("vote")
      currentSender = sender()
      currentSong = song
      // vote for itself
      if (sender == self) {
        println(song)
        sender ! agree(song)
      }
      else{
        println("- POP -" + song)

        // define the JavaFX ec so we can use it explicitly
        FXUtils.runAndWait {
          println("- THREAD PROBLEM SOVLED? -")
          Main.playList.voting(song)
        }
      }

    case ReceiveTimeout =>
      println("- TIME OUT -")
      context.setReceiveTimeout(Duration.Undefined)
      mediator ! Publish ("content", new timeOut())
      mediator ! Publish("content",new release())

    case timeOut() =>
      println("- TIME OUT FROM REMOTE -")
      FXUtils.runAndWait {
        Main.playList.closePop()
      }


    // vote agree/ disagree
    case localAgree()=>
      println("local agree")
      currentSender ! agree(currentSong)

    case localReject()=>
      currentSender ! reject()



    case bytes: Array[Byte] =>
      val output:Output = Resource.fromFile("someFile")


    case SubscribeAck(Subscribe("content", None, `self`)) =>
      println("subAck")
    // context become ready

    case songModel:SongModel =>
      println("- CHECK - SONGMODEL RECEIVED ")
      songModel.mediaPlayer().play()

    // receive voting, agree or reject
    case agree(src) =>
      println("- AGREE - src: "+src)
      val cursize = cluster.state.members.filter(_.status == MemberStatus.Up).size
      countAgree+=1

      println("agree"+ countAgree + "size"+ cursize)
      if (countAgree > (cursize/2) && !hassend){
        context.setReceiveTimeout(Duration.Undefined)
        println("send")
        hassend=true
        mediator ! Publish("content",transferMusic())
//        val time = getNTPTime()
//        mediator ! Publish("content",startTime(time+10000, currentSong))
//        mediator ! Publish("content",new release())
        println("over")
      }
//      else if ((cursize%2==0) && (countAgree == cursize/2) && !hassend){
//        context.setReceiveTimeout(Duration.Undefined)
//        println("send")
//        hassend=true
//        mediator ! Publish("content",transferMusic())
//        val time = getNTPTime()
//        mediator ! Publish("content",startTime(time+10000,src))
//        mediator ! Publish("content",new release())
//        println("over")
//      }

    case reject()=>
      println("reject")
      val cursize = cluster.state.members.filter(_.status == MemberStatus.Up).size
      countReject += 1
      if ((cursize%2==0) && countReject >= (cursize/2)){
        context.setReceiveTimeout(Duration.Undefined)
        mediator ! Publish("content",new release())
      }
      else if ((cursize%2==1) && countReject > (cursize/2)){
        context.setReceiveTimeout(Duration.Undefined)
        mediator ! Publish("content",new release())
      }

    case transferMusic()=>
      println("- MOCK TRANSFER ... -")
      FXUtils.runAndWait {
        Main.playList.closePop()
      }
      if (sender == self){

        val f = new File("./"+currentSong+".mp3")

        val input = new FileInputStream(f)
        var flag= false
        var data = new ArrayBuffer[Int]

        val fu= Future {
          while(!flag){
            var in = input.read()
            if (in == -1)
              flag = true
            else{
              //mediator ! Publish("content", new testfile(in))
              data.+=(in)
            }
          }
        }
        fu onSuccess{
          case _: Unit =>
            println("succ")
            mediator ! Publish("content", new testf2(data))
            input.close()
        }

        println("send over")
      }

    case testf2(data: ArrayBuffer[Int]) =>
      println("2 begin")
     // scala.io.StdIn.readInt()
      if (sender != self){
        val file = new File("./"+currentSong+".mp3")
        val output = new FileOutputStream(file)
        val fu = Future{
          for (in <- data){
            output.write(in)
          }
        }
        fu onSuccess{
          case _: Unit =>
            println("receive succ")
            output.close()
            currentSender ! receiveOver()
        }
      }
      else{
        sender ! receiveOver()
      }

      println("2 done")
      println(System.currentTimeMillis())

    case receiveOver()=>
      println("receive OVer")
      countRev+=1
      val cursize = cluster.state.members.filter(_.status == MemberStatus.Up).size
      if (countRev >= cursize){
        val time = getNTPTime()
        mediator ! Publish("content",startTime(time+10000, currentSong))
        mediator ! Publish("content",new release())
      }

    case s:startTime=>
      var cur = getNTPTime()
      println("start count down"+s.t + "current" + cur)
      val list = new PlayList()
      list.select(s.name) // update name
      println("- CHECK -  SRC: "+s.name+" REAL: "+MusicName.name)
      val url:String = new File("./"+MusicName.name+".mp3").toURI().toString()
      println("URL: "+url)
      FXUtils.runAndWait {
        SongModel.url = url
      }
      while (s.t > cur){
        cur += 1
      }
      println("start play"+cur)
      SongModel.mediaPlayer().play()
      hassend=false

    case s: String ⇒
      log.info("Got {}", s)

    case _: MemberEvent => // ignore
  }

  def printToFile(f: java.io.File)(op: java.io.PrintWriter => Unit) {
    val p = new java.io.PrintWriter(f)
    try { op(p) } finally { p.close() }
  }

  def getNTPTime():Long= {
    val client = new NTPUDPClient();
    // We want to timeout if a response takes longer than 5 seconds
    client.setDefaultTimeout(5000);

    try {
      val hostAddr = InetAddress.getByName("ntp02.oal.ul.pt")
      val info = client.getTime(hostAddr);
      val date = new Date(info.getReturnTime());
      println(date)
      return date.getTime;
    }
    catch {
      case e:Exception=>
        client.close();
        e.printStackTrace();
    }
    //   client.close();
    return -1;
  }
}