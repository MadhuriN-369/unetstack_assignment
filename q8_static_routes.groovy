import org.arl.fjage.*
import org.arl.unet.*
import org.arl.unet.net.*
import org.arl.unet.sim.channels.*
platform = RealTimePlatform
def useRoutes = (System.getenv('USE_ROUTES') ?: '0') == '1'
channel.model = ProtocolChannelModel
channel.communicationRange = 1000.m
channel.detectionRange     = 1200.m
channel.interferenceRange  = 1500.m
simulate {
  def n11 = node '11', address: 11, location: [0,    0, -15.m], web: 8081, api: 1101, stack: "$home/etc/setup"
  def n12 = node '12', address: 12, location: [900,  0, -15.m], web: 8082, api: 1102, stack: "$home/etc/setup"
  def n13 = node '13', address: 13, location: [1800, 0, -15.m], web: 8083, api: 1103, stack: "$home/etc/setup"
  if (useRoutes) {   // static routes from the script, at startup
    n11.startup = { agentForService(Services.ROUTING) << new EditRouteReq(to: 13, nextHop: 12, hops: 2, op: RouteInfo.Operation.CREATE) }
    n13.startup = { agentForService(Services.ROUTING) << new EditRouteReq(to: 11, nextHop: 12, hops: 2, op: RouteInfo.Operation.CREATE) }
  }
}
/* Node 11 shell:
     router << new DatagramReq(to: 13, protocol: Protocol.USER, data: 'hi'.bytes)    // fails w/o routes
     ping 13                                                                           // timeout w/o routes
     routes                                    // on 11 and on 13
     trace 13                                  // expect [11, 12, 13, 12, 11]  -> proves packet passed via 12
     ping 13
   Node 13 shell: router << new DatagramReq(to: 11, protocol: Protocol.USER, data: 'reply'.bytes)
   Also: tail / logs/log-0.txt and logs/trace.json show activity at node 12.                              */
