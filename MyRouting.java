/*******************

Team members and IDs:
Victoria Lariot - 6124058
Martin Alvarez - 5856597
Gretel Gomez Rodriguez - 6174028
Github link:
https://github.com/xxx/yyy

*******************/

package net.floodlightcontroller.myrouting;

import java.util.Collection;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.devicemanager.SwitchPort;

import java.util.ArrayList;
import java.util.Set;

import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryListener;
import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryService;
import net.floodlightcontroller.linkdiscovery.LinkInfo;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.routing.Link;
import net.floodlightcontroller.routing.Route;
import net.floodlightcontroller.routing.RouteId;
import net.floodlightcontroller.staticflowentry.IStaticFlowEntryPusherService;
import net.floodlightcontroller.topology.NodePortTuple;

import org.openflow.util.HexString;
import org.openflow.util.U8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyRouting implements IOFMessageListener, IFloodlightModule {

	protected IFloodlightProviderService floodlightProvider;
	//protected IFloodlightService floodlightService; // possibly delete
	protected Set<Long> macAddresses;
	protected static Logger logger;
	protected IDeviceService deviceProvider;
	protected ILinkDiscoveryService linkProvider;

	protected Map<Long, IOFSwitch> switches;
	protected Map<Link, LinkInfo> links;
	protected Collection<? extends IDevice> devices;

	protected static int uniqueFlow;
	protected ILinkDiscoveryService lds;
	protected IStaticFlowEntryPusherService flowPusher;
	protected boolean printedTopo = false;
	protected ArrayList<ArrayList> neighbors = new ArrayList<ArrayList>();
	//protected ILinkDiscoveryService linkDiscoveryProvider; //possibly delete this
	

	@Override
	public String getName() {
		return MyRouting.class.getSimpleName();
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		return (type.equals(OFType.PACKET_IN)
				&& (name.equals("devicemanager") || name.equals("topology")) || name
					.equals("forwarding"));
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		return false;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		return null;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		return null;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
		//floodlightService.add(ILinkDiscoveryService.class);//possibly delet
		l.add(IFloodlightProviderService.class);
		l.add(IDeviceService.class);
		l.add(ILinkDiscoveryService.class);
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context)
			throws FloodlightModuleException {
		floodlightProvider = context
				.getServiceImpl(IFloodlightProviderService.class);
		deviceProvider = context.getServiceImpl(IDeviceService.class);
		linkProvider = context.getServiceImpl(ILinkDiscoveryService.class);
		flowPusher = context
				.getServiceImpl(IStaticFlowEntryPusherService.class);
		lds = context.getServiceImpl(ILinkDiscoveryService.class);
		//linkDiscoveryProvider = context.getServiceImpl(ILinkDiscoveryService.class); //possibly delete

	}

	@Override
	public void startUp(FloodlightModuleContext context) {
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
	}

	@Override
	public net.floodlightcontroller.core.IListener.Command receive(
			IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {

		
		
		// Print the topology if not yet.
		if (!printedTopo) {
			System.out.println("*** Print topology");
//			System.out.println(floodlightProvider.getAllSwitchMap()); //this gives us switch #s and ipv4 addresses and port #s
//			System.out.println(floodlightProvider.getAllSwitchDpids()); // this gives us the switch #s
//			System.out.println(floodlightProvider.getSwitch(1)); // prints info for given switch
//			System.out.println(linkProvider.getSwitchLinks());
//			System.out.println(lds.getSwitchLinks());
			
			//System.out.println(linkProvider.);
			Map<Long, Set<Link>> links = linkProvider.getSwitchLinks();
			/*for (Long link : links.keySet()) {
	            System.out.println(link);
	        }*/
			
			ArrayList<Long> rlinks = new ArrayList<Long>();
			
			for (Map.Entry<Long, Set<Link>> me : links.entrySet()) { 
	            Set<Link> linkar = me.getValue(); //Set that holds the link values for me
	            
	            Iterator<Link> it = linkar.iterator(); //iterates through the link values
	            while(it.hasNext()){
	               //System.out.println(it.next());
	            	
	               long r = it.next().getSrc();// grabs the source address
	               if( !rlinks.contains(r) && r!= me.getKey()) {
	               rlinks.add(r);
	               }
	               
	            }
	            
	            
	            
	            System.out.print("Switch " + me.getKey() +" neighbors: "/*+ rlinks*/);
	            for(int i = 0; i < rlinks.size(); i ++) {
	            	if(i == rlinks.size()-1) {
	            		System.out.print(rlinks.get(i));
	            		
	            	}
	            	
	            	else {
	            		System.out.print(rlinks.get(i)+ ", ");
	            	}
	            }
	            
	            System.out.println();
	            neighbors.add((ArrayList)rlinks.clone());
	            
	            rlinks.clear();
	            //System.out.println(me.getValue()); 
	            
	        }
			
			// make sure to uncomment this
			//for(Long l : links)
			//System.out.println(linkProvider.get)
			
			/*for (Link key : links.keySet()) {
	            System.out.println(key);
	        }*/
			//System.out.println(links.keySet().toString());
			
			/*Iterator iterator = switches.entrySet().iterator();
			while (iterator.hasNext()) 
			{
			    Map.Entry mapEntry = (Map.Entry) iterator.next();
			System.out.println("The key is: " + mapEntry.getKey() + ",value is :" + mapEntry.getValue());

			} */
			//System.out.println(sw.getStringId());
			
			

			// For each switch, print its neighbor switches.
			//for ( : switches)

			printedTopo = true;
		}


		// eth is the packet sent by a switch and received by floodlight.
		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx,
				IFloodlightProviderService.CONTEXT_PI_PAYLOAD);

		// We process only IP packets of type 0x0800.
		if (eth.getEtherType() != 0x0800) {
			return Command.CONTINUE;
		}
		else{
			System.out.println("*** New flow packet");

			// Parse the incoming packet.
			OFPacketIn pi = (OFPacketIn)msg;
			OFMatch match = new OFMatch();
		    match.loadFromPacket(pi.getPacketData(), pi.getInPort());
		    String sourceIP= match.getNetworkSourceCIDR().substring(0, match.getNetworkSourceCIDR().indexOf("/"));
		    String destinationIP = match.getNetworkDestinationCIDR().substring(0, match.getNetworkDestinationCIDR().indexOf("/"));
		    
		   
		    
		    
		 
		    //System.out.println(match.getInputPort());

		    
			
			// Obtain source and destination IPs.
			// ...
			System.out.println("srcIP: " + sourceIP);
	        System.out.println("dstIP: " + destinationIP);

	        System.out.println(neighbors);
			// Calculate the path using Dijkstra's algorithm.
			Route route = null;
			ArrayList<Long>SPT = new <Long>ArrayList();
			ArrayList<Long>Current = new <Long>ArrayList();
			ArrayList<Long>StartList = new <Long>ArrayList();
			//System.out.println(neighbors.get(1));
			int points = 0;
			int start = Integer.parseInt(sourceIP.substring(sourceIP.lastIndexOf(".")+1));
			int end =Integer.parseInt(destinationIP.substring(destinationIP.lastIndexOf(".")+1));
			StartList = neighbors.get(start-1);
			System.out.println(StartList);
			Set<Long> vertices = floodlightProvider.getAllSwitchDpids();
			
			
			// ...
			System.out.println("route: " + "1 2 3 ...");
			dijkstras(start, end, vertices);

			// Write the path into the flow tables of the switches on the path.
			if (route != null) {
				installRoute(route.getPath(), match);
			}
			
			return Command.STOP;
		}
	}
	public long dijkstras(int current, int destination, Set<Long> vertices) {
		Set<Long> settled = new HashSet<>();
		
		ArrayList<Integer>distance = new <Integer>ArrayList();
		for( int i = 0; i < vertices.size(); i++) {
			distance.add(Integer.MAX_VALUE);
		}
		
		//int points = 0;
		
		ArrayList<Long>StartList = new <Long>ArrayList();
		//settled.add((long)current);
		StartList = neighbors.get(current);
		for (long l : StartList) {
			distance.set((int)l-1,costCalculator(current, l));
				
		}
		System.out.println(distance);
		
		
		
		return 3;
	}
	public int costCalculator(long start, long end) {
		if ( start% 2 != 0 && end %2 != 0) 
			return 1;
		else if ( start %2 == 0 && end % 2 == 0)
			return 100;
		else 
			return 10;
	}
	
	// Install routing rules on switches. 
	private void installRoute(List<NodePortTuple> path, OFMatch match) {

		OFMatch m = new OFMatch();

		m.setDataLayerType(Ethernet.TYPE_IPv4)
				.setNetworkSource(match.getNetworkSource())
				.setNetworkDestination(match.getNetworkDestination());

		for (int i = 0; i <= path.size() - 1; i += 2) {
			short inport = path.get(i).getPortId();
			m.setInputPort(inport);
			List<OFAction> actions = new ArrayList<OFAction>();
			OFActionOutput outport = new OFActionOutput(path.get(i + 1)
					.getPortId());
			actions.add(outport);

			OFFlowMod mod = (OFFlowMod) floodlightProvider
					.getOFMessageFactory().getMessage(OFType.FLOW_MOD);
			mod.setCommand(OFFlowMod.OFPFC_ADD)
					.setIdleTimeout((short) 0)
					.setHardTimeout((short) 0)
					.setMatch(m)
					.setPriority((short) 105)
					.setActions(actions)
					.setLength(
							(short) (OFFlowMod.MINIMUM_LENGTH + OFActionOutput.MINIMUM_LENGTH));
			flowPusher.addFlow("routeFlow" + uniqueFlow, mod,
					HexString.toHexString(path.get(i).getNodeId()));
			uniqueFlow++;
		}
	}
}
