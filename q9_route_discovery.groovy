import org.arl.fjage.RealTimePlatform
import org.arl.unet.sim.channels.*
platform = RealTimePlatform
def remove3 = (System.getenv('NO3') ?: '0') == '1'
channel.model = ProtocolChannelModel
channel.communicationRange = 1000.m
channel.detectionRange     = 1200.m
channel.interferenceRange  = 1500.m
simulate {
  node '1', address: 1, location: [0,    0, -15.m], web: 8081, api: 1101, stack: "$home/etc/setup"
  node '2', address: 2, location: [900,  0, -15.m], web: 8082, api: 1102, stack: "$home/etc/setup"
  if (!remove3) node '3', address: 3, location: [1800, 0, -15.m], web: 8083, api: 1103, stack: "$home/etc/setup"
  node '4', address: 4, location: [2700, 0, -15.m], web: 8084, api: 1104, stack: "$home/etc/setup"
}
/* Node 1 shell:
     subscribe rdp                       // to see RouteDiscoveryNtf
     rreq 4, 3, 3, 20                    // up to 3 hops, 3 probes, 20 s apart (or: rdp << new RouteDiscoveryReq(to: 4))
     routes                              // wait ~1-2 min; look for to=4 nextHop=2 hops=3
     trace 4                             // expect [1, 2, 3, 4, 3, 2, 1]
     router << new DatagramReq(to: 4, protocol: Protocol.USER, data: 'hello'.bytes)
   Node 4 shell: receives DatagramNtf (shell subscribes to Topics.DATAGRAM automatically)
   NO3=1: repeat rreq 4 -> no route to 4 appears (gap 1800 m > 1000 m range).                    */
