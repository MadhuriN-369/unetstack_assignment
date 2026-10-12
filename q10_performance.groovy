import org.arl.fjage.*
import org.arl.unet.*
import org.arl.unet.net.*
import org.arl.unet.sim.channels.*

class Stats { static Map sent = [:]; static Map recvd = [:]; static List delays = [] }

class SinkAgent extends UnetAgent {          // on destination: records receive time
  @Override void startup() { subscribe topic(Topics.DATAGRAM) }
  @Override void processMessage(Message msg) {
    if (msg instanceof DatagramNtf && msg.protocol == Protocol.USER) {
      int seq = Integer.parseInt(new String(msg.data).split(':')[0])
      double t = currentTimeMillis()/1000.0
      if (!Stats.recvd.containsKey(seq)) {
        Stats.recvd[seq] = t
        Stats.delays << (t - (double)Stats.sent[seq])
      }
    }
  }
}

platform = DiscreteEventSimulator
def dest = (System.getenv('DEST') ?: '2') as int
int N = 50, PERIOD = 20, PAYLOAD = 8          // 8-byte payload: "001:xxxx"
channel.model = ProtocolChannelModel
channel.communicationRange = 1000.m
channel.detectionRange     = 1200.m
channel.interferenceRange  = 1500.m
int T = N * PERIOD + 120                      // run time (s), leaves time for last packet

simulate T, {
  def n1 = node '1', address: 1, location: [0,    0, -15.m], stack: "$home/etc/setup"
  def n2 = node '2', address: 2, location: [900,  0, -15.m], stack: "$home/etc/setup"
  def n3 = node '3', address: 3, location: [1800, 0, -15.m], stack: "$home/etc/setup"
  def n4 = node '4', address: 4, location: [2700, 0, -15.m], stack: "$home/etc/setup"
  def nodes = [2: n2, 3: n3, 4: n4]
  n1.startup = {                               // static routes via EditRouteReq
    def r = agentForService(Services.ROUTING)
    r << new EditRouteReq(to: 2, nextHop: 2, hops: 1, op: RouteInfo.Operation.CREATE)
    r << new EditRouteReq(to: 3, nextHop: 2, hops: 2, op: RouteInfo.Operation.CREATE)
    r << new EditRouteReq(to: 4, nextHop: 2, hops: 3, op: RouteInfo.Operation.CREATE)
    int seq = 0
    add new TickerBehavior(PERIOD * 1000, {
      if (seq < N) {
        seq++
        Stats.sent[seq] = currentTimeMillis()/1000.0
        r << new DatagramReq(to: dest, protocol: Protocol.USER, data: String.format('%03d:xxxx', seq).bytes)
      }
    })
  }
  n2.startup = {
    if (dest == 2) container.add 'sink', new SinkAgent()      // sink merged into the SAME startup closure
    def r = agentForService(Services.ROUTING)
    r << new EditRouteReq(to: 3, nextHop: 3, hops: 1, op: RouteInfo.Operation.CREATE)
    r << new EditRouteReq(to: 4, nextHop: 3, hops: 2, op: RouteInfo.Operation.CREATE)
    r << new EditRouteReq(to: 1, nextHop: 1, hops: 1, op: RouteInfo.Operation.CREATE)
  }
  n3.startup = {
    if (dest == 3) container.add 'sink', new SinkAgent()
    def r = agentForService(Services.ROUTING)
    r << new EditRouteReq(to: 4, nextHop: 4, hops: 1, op: RouteInfo.Operation.CREATE)
    r << new EditRouteReq(to: 2, nextHop: 2, hops: 1, op: RouteInfo.Operation.CREATE)
    r << new EditRouteReq(to: 1, nextHop: 2, hops: 2, op: RouteInfo.Operation.CREATE)
  }
  n4.startup = {
    if (dest == 4) container.add 'sink', new SinkAgent()
    def r = agentForService(Services.ROUTING)
    r << new EditRouteReq(to: 3, nextHop: 3, hops: 1, op: RouteInfo.Operation.CREATE)
    r << new EditRouteReq(to: 2, nextHop: 3, hops: 2, op: RouteInfo.Operation.CREATE)
    r << new EditRouteReq(to: 1, nextHop: 3, hops: 3, op: RouteInfo.Operation.CREATE)
  }
}

int sent = Stats.sent.size(), rx = Stats.recvd.size()
double avg = Stats.delays ? ((double)Stats.delays.sum())/Stats.delays.size() : 0
double thr = rx * PAYLOAD * 8.0 / (N * PERIOD)           // delivered payload bits / traffic duration
println sprintf('hops=%d sent=%d received=%d PDR=%.1f%% avgDelay=%.3f s throughput=%.4f bit/s',
  [dest-1, sent, rx, 100.0*rx/sent, avg, thr])
