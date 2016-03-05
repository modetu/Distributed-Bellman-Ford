import java.util.*;
import java.net.*;
import java.io.*;

public class Client{

	static ArrayList<Neighbors> neighborClient=new ArrayList<Neighbors>();
	static ArrayList<Neighbors> wholeClient=new ArrayList<Neighbors>();
	static ArrayList<Neighbors> distanceVector=new ArrayList<Neighbors>();
	static HashMap<List,Float> nearbyCostList=new HashMap<List, Float>();
	static HashMap<List,Float> totalCostList=new HashMap<List,Float>();
	static HashMap<List,List> clientlinkList=new HashMap<List,List>();
	static HashMap<List,Float> linkDownRecord=new HashMap<List,Float>();
	static DatagramSocket clientSocket;
    static Timer timer;
    static String localIP;
    static Integer localport;
    static SendThread sendDVTask;
    static List localTuple=new ArrayList();
    static float infinity=Float.POSITIVE_INFINITY;
	// static ArrayList<Neighbors> netAllClients=new ArrayList<Neighbors>();

	public static void main(String args[]){
		
		timer=new Timer();
		try {
			localIP = InetAddress.getLocalHost().getHostAddress();
			System.out.println("localIPT: "+localIP);
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		}

		localport=Integer.parseInt(args[0]);
		int timeout=Integer.parseInt(args[1]);

		try{
			clientSocket=new DatagramSocket(localport);
		}catch(SocketException e){
			e.printStackTrace();
		}

		localTuple.add(localIP);
		localTuple.add(localport);
		System.out.println(localTuple.toString());
		
		//record original neighbors
		for(int i=2;i<args.length;){
			String ipaddress=args[i];			
			int port=Integer.parseInt(args[i+1]);
			float ncost=Float.parseFloat(args[i+2]);
			float weight=ncost;
			List linkct=new ArrayList();
			linkct.add(ipaddress);
			linkct.add(port);
			Neighbors nb=new Neighbors(ipaddress, port, linkct);

			//update all record List
			nearbyCostList.put(linkct, ncost);
			totalCostList.put(linkct,weight);
			neighborClient.add(nb);
			distanceVector.add(nb);
			wholeClient.add(nb);
			clientlinkList.put(linkct, linkct);
			i=i+3;

		}


		sendDVTask=new SendThread();
		timer.schedule(sendDVTask, 0, timeout*1000);

		Thread receiveDVTask = new Thread(new ReceiveThread());
        receiveDVTask.start();

        //Command:
        String command;
        Scanner in=new Scanner(System.in);
        while(true){
        	command=in.nextLine();
        	String[] commandMsg=command.split(" ");
        	if(commandMsg[0].equals("SHOWRT")){
        		showRT(distanceVector);
        	}else if(commandMsg[0].equals("LINKDOWN")){
        		String linkdownip=commandMsg[1];
        		Integer linkDownport=Integer.parseInt(commandMsg[2]);
        		linkDown(linkdownip, linkDownport);
        	}else if(commandMsg[0].equals("LINKUP")){
        		String linkupip=commandMsg[1];
        		Integer linkupport=Integer.parseInt(commandMsg[2]);
        		linkUp(linkupip, linkupport);
        	}else if(commandMsg[0].equals("CLOSE")){
        		close();
        	}

        }
	}

	public static void showRT(ArrayList distanceVector){
		if(distanceVector!=null){
			int dvSize=distanceVector.size();
			String output=null;
			for(int i=0;i<dvSize;i++){
				
				Neighbors nbs=(Neighbors)distanceVector.get(i);
				List nbstuple=nbs.getLocalMsg();
				List nbslinktuple=clientlinkList.get(nbstuple);
				String link="("+nbslinktuple.get(0)+":"+nbslinktuple.get(1).toString()+")";
				String s="Destination = " + nbs.getIP() + ":" +
			        nbs.getPort() + ", " + "Cost = " + 
			        totalCostList.get(nbstuple)+", "+"NearbyCost: "+nearbyCostList.get(nbstuple) +", "+ link; 
			    System.out.println(s);
			}
		}
		
	}

	public static void linkDown(String ip, int port){
		Float cutdown=Float.POSITIVE_INFINITY;
		List linkdownTuple=new ArrayList();
		linkdownTuple.add(ip);
		linkdownTuple.add(port);
		Float linkdowncost=nearbyCostList.get(linkdownTuple);
		linkDownRecord.put(linkdownTuple, linkdowncost);
		nearbyCostList.put(linkdownTuple, cutdown);
		totalCostList.put(linkdownTuple,cutdown);

		//send message to the other client of this linkdown edge
		String linkdownMsg="LINKDOWN"+" "+localTuple.get(0)+" "+localTuple.get(1)+" ";
		byte[] ldMsgByte=linkdownMsg.getBytes();
		DatagramPacket linkdownMsgPkt=null;
		try{
			linkdownMsgPkt=new DatagramPacket(ldMsgByte, ldMsgByte.length, InetAddress.getByName(ip), port);
		}catch(UnknownHostException e){
			e.printStackTrace();
		}
		try{
			clientSocket.send(linkdownMsgPkt);
		}catch(IOException e){
			e.printStackTrace();
		}
		//showRT(distanceVector);

		int nbsize=neighborClient.size();
		int recordIndex=-1;
		int dvsizel=distanceVector.size();

		//set totalcost of clients which link through this linkdown edge to infinity
		for(int i=0;i<dvsizel;i++){
			List dvtuplel=distanceVector.get(i).getLocalMsg();
			String dvip=(String)dvtuplel.get(0);
			if(dvtuplel.equals(linkdownTuple)){
				continue;
			}

			List linkMsg=clientlinkList.get(dvtuplel);
			if(linkMsg.equals(linkdownTuple)){
				totalCostList.put(dvtuplel, cutdown);
				String setInfinity="SETINFINITY"+" "+localTuple.get(0)+" "+localTuple.get(1)+" ";
				byte[] setInfinityBytes=setInfinity.getBytes();
				DatagramPacket setInfinityPacket=null;
				try{
					setInfinityPacket=new DatagramPacket(setInfinityBytes, setInfinityBytes.length, 
						InetAddress.getByName(dvip), (int)dvtuplel.get(1));
				}catch(UnknownHostException e){
					e.printStackTrace();
				}

				try{
					clientSocket.send(setInfinityPacket);
				}catch(IOException e){
					e.printStackTrace();
				}
				clientlinkList.put(dvtuplel, dvtuplel);
			}

		}

		//remove this client
		for(int i=0;i<nbsize;i++){
			List nbtuple=neighborClient.get(i).getLocalMsg();
			if(nbtuple.equals(linkdownTuple)){
				recordIndex=i;
			}
		}
		neighborClient.remove(recordIndex);

	}


	public static void linkUp(String ip, int port){
		//Get the original cost of this edge
		List linkUpTuple=new ArrayList();
		linkUpTuple.add(ip);
		linkUpTuple.add(port);
		Float costOriginal=linkDownRecord.get(linkUpTuple);

		//Set the nearby cost again and add this client to neighbors List
		nearbyCostList.put(linkUpTuple, costOriginal);
		Neighbors ng=new Neighbors(ip, port, linkUpTuple);
		neighborClient.add(ng);

		//Send message to the other client of this linkup edge
		String linkupMsg="LINKUP"+" "+localIP+" "+localport+" ";
		byte[] linkupMsgBytes=linkupMsg.getBytes();
		DatagramPacket linkupPacket=null;
		try{
			linkupPacket=new DatagramPacket(linkupMsgBytes, linkupMsgBytes.length, InetAddress.getByName(ip), port);
		}catch(UnknownHostException e){
			e.printStackTrace();
		}
		try{
			clientSocket.send(linkupPacket);
		}catch(IOException e){
			e.printStackTrace();
		}


	}

	public static void close(){
		
		while(neighborClient.size()!=0){
			List neighborclose=neighborClient.get(0).getLocalMsg();
			String closeip=(String)neighborclose.get(0);
			int closeport=(int)neighborclose.get(1);
			linkDown(closeip, closeport);
			try{
				Thread.sleep(100);
			}catch(InterruptedException e){
				e.printStackTrace();
			}

		}
		System.exit(1);
			
		
	}


	//Send Distance Vector message to nearby Client in this network
	public static class SendThread extends TimerTask{
		@Override
		public void run(){

			int sizenb=neighborClient.size();
				
			//send local message to all neighbors 
			for(int i=0;i<sizenb;i++){
				List tuple=neighborClient.get(i).getLocalMsg();
				float nearbyc=nearbyCostList.get(tuple);
				String localMsgSend="LOCALMSG"+" "+localTuple.get(0)+" "+localTuple.get(1)+" "+nearbyc+" ";
				byte[] localMsgPkt=localMsgSend.getBytes();
				DatagramPacket p=null;
				try{
					p=new DatagramPacket(localMsgPkt, localMsgPkt.length, 
						InetAddress.getByName((String)tuple.get(0)), (int)tuple.get(1));
				}catch (UnknownHostException e1) {	
					e1.printStackTrace();
				}

				try {
					clientSocket.send(p);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			//send DV message
			if(distanceVector.size()!=0){
				int sizetp=distanceVector.size();

				//packet DistanceVector message in List
				List packetList=new ArrayList<byte[]>();
				for(int i=0;i<sizetp;i++){
					Neighbors flg=(Neighbors)distanceVector.get(i);
					List flgtuble=flg.getLocalMsg();
					List flgl=clientlinkList.get(flgtuble);
					String msg="UPDATEDV"+" "+localIP+" "+localport.toString()+" "+flg.getIP()+" "
						+flg.getPort()+" "+totalCostList.get(flgtuble)+" ";
					byte[] packet=msg.getBytes();
					packetList.add(packet);			
				}
			
				//Send packets to nearby clients
				if(neighborClient!=null){
					
					for(int i=0;i<sizenb;i++){
						Neighbors nnb=neighborClient.get(i);
						List nnbtuple=nnb.getLocalMsg();
						String nip=nnb.getIP();
						int nport=nnb.getPort();

						for(int j=0;j<packetList.size();j++){
							byte[] pk=(byte[]) packetList.get(j);
							DatagramPacket packet=null;
							try {
								packet = new DatagramPacket(pk, pk.length, InetAddress.getByName(nip), nport);
							} catch (UnknownHostException e1) {	
								e1.printStackTrace();
							}							
							try {
								clientSocket.send(packet);
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
						
					}

				}
				
			}
		}
		
	}


	public static class ReceiveThread implements Runnable{
		ArrayList<byte[]> packetReceived=new ArrayList<byte[]>();
		@Override
		public void run(){
			while(true){
				//System.out.println(2);
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				//receive packets
				try {	
					byte[] bt=new byte[7000];	
					DatagramPacket receiverPacket=new DatagramPacket(bt,bt.length);			
					clientSocket.receive(receiverPacket);
					//System.out.println(0);
					byte[] packetR=receiverPacket.getData();
					packetReceived.add(packetR);
					analysePacket(packetR);
				} catch (IOException e) {
					e.printStackTrace();
				}
							
			}
			
		}

		public void analysePacket(byte[] packet) {
			String str=new String(packet);
			String[] format=str.split(" ");
			int strSize=format.length;
			//System.out.println("format[6]: "+format[6]);
		 	
			//System.out.println("textlocalip: " + localIP);
			//parse packet information 
			//packet message
			String flagMsg=format[0];
			//System.out.println(flgMsg);
			if(flagMsg.equals("UPDATEDV")){
				// System.out.println(1);

				//split packet message

				//sender Tuple
				List sendertuple=new ArrayList();
				sendertuple.add(format[1]);
				sendertuple.add(Integer.parseInt(format[2]));

				float costToSender=nearbyCostList.get(sendertuple);
				//DV Client tuple
				List cltuple=new ArrayList();
				cltuple.add(format[3]);
				cltuple.add(Integer.parseInt(format[4]));

				//DV cost
				float totalCostSend=Float.parseFloat(format[5]);
				float flagtotal=costToSender+totalCostSend;

				//remove the local client in message
				if(!cltuple.equals(localTuple)){
					//DV contains this client already
					if(totalCostList.containsKey(cltuple)){
						float originalCost=totalCostList.get(cltuple);					
						if(originalCost>flagtotal){
							//update totalcost message
							totalCostList.put(cltuple, flagtotal);
							clientlinkList.put(cltuple, sendertuple);
						}

					//DV Does not contain this client, add!
					}else{
						totalCostList.put(cltuple, flagtotal);
						Neighbors newclt=new Neighbors((String)cltuple.get(0), (int)cltuple.get(1), sendertuple);
						distanceVector.add(newclt);
						clientlinkList.put(cltuple, sendertuple);

					}

				}

			//Neighbor message
			}else if(flagMsg.equals("LOCALMSG")){
				List neigbortuple=new ArrayList();
				neigbortuple.add(format[1]);
				neigbortuple.add(Integer.parseInt(format[2]));
				float neighborcost=Float.parseFloat(format[3]);

				//A new neighbor
				if(!nearbyCostList.containsKey(neigbortuple)){
					//add this new neighbor
					nearbyCostList.put(neigbortuple, neighborcost);
					Neighbors newnb=new Neighbors((String)neigbortuple.get(0), (int)neigbortuple.get(1), neigbortuple);
					neighborClient.add(newnb);

					//DV contains this new neighbor
					if(totalCostList.containsKey(neigbortuple)){
						//update DV if possible
						if(totalCostList.get(neigbortuple)>neighborcost){
							totalCostList.put(neigbortuple,neighborcost);
							clientlinkList.put(neigbortuple, neigbortuple);
							showRT(distanceVector);
						}

					//DV does not contain this new neighbor, add!
					}else{
						//System.out.println("new neighbor!");
						totalCostList.put(neigbortuple,neighborcost);
						distanceVector.add(newnb);
						clientlinkList.put(neigbortuple, neigbortuple);
						//showRT(distanceVector);
					}

				//This neigbor is in nearbyList
				}else{
					float originalTotalcost=totalCostList.get(neigbortuple);
					if(originalTotalcost>neighborcost){
						totalCostList.put(neigbortuple, neighborcost);
						clientlinkList.put(neigbortuple,neigbortuple);
					}
				}

			//LinkDown in some link
			}else if(flagMsg.equals("LINKDOWN")){
				List sendertuple=new ArrayList();
				sendertuple.add(format[1]);
				sendertuple.add(Integer.parseInt(format[2]));
				//System.out.println("test linkdown: "+format[1]+" "+format[2]);
				//System.out.println(sendertuple);

				//Reord this linkdown edges
				Float recordCost=totalCostList.get(sendertuple);
				linkDownRecord.put(sendertuple, recordCost);

				//set nearbycost and totalcost of this client to infinity
				Float cutdownl=Float.POSITIVE_INFINITY;
				totalCostList.put(sendertuple, cutdownl);
				nearbyCostList.put(sendertuple, cutdownl);

				//System.out.println(nearbyCostList);
				//System.out.println(totalCostList);
				//System.out.println(2+nearbyCostList.get(sendertuple));
				int dvsizeld=distanceVector.size();
				int recordIndex=-1;
				int ncs=neighborClient.size();

				//set totalcost of clients which linktrough the linkdown edge to infinity
				for(int i=0;i<dvsizeld;i++){
					List dvTuple=distanceVector.get(i).getLocalMsg();
					List dvlinkTuple=clientlinkList.get(dvTuple);
					String dvip=(String)dvTuple.get(0);
					if(dvlinkTuple.equals(sendertuple)){
						totalCostList.put(dvTuple, cutdownl);
						clientlinkList.put(dvTuple, dvTuple);
						String setInfinity="SETINFINITY"+" "+localTuple.get(0)+" "+localTuple.get(1)+" ";
						byte[] setInfinityBytes=setInfinity.getBytes();
						DatagramPacket setInfinityPacket=null;
						try{
							setInfinityPacket=new DatagramPacket(setInfinityBytes, setInfinityBytes.length, 
								InetAddress.getByName(dvip), (int)dvTuple.get(1));
						}catch(UnknownHostException e){
							e.printStackTrace();
						}

				try{
					clientSocket.send(setInfinityPacket);
				}catch(IOException e){
					e.printStackTrace();
				}
					}
				}

				//remove this client from neighborClient list
				for(int i=0;i<ncs;i++){
					List nbtuple=neighborClient.get(i).getLocalMsg();
					if(nbtuple.equals(sendertuple)){
						recordIndex=i;
					}
				}
				neighborClient.remove(recordIndex);

			//Linkdown a edge and need to update DV
			}else if(flagMsg.equals("SETINFINITY")){
				List sendertuple=new ArrayList();
				sendertuple.add(format[1]);
				sendertuple.add(Integer.parseInt(format[2]));
				totalCostList.put(sendertuple, infinity);

			//Linkuo a edge and need to update NearbyCost List and nearby neighbors list
			}else if(flagMsg.equals("LINKUP")){
				List sendertuple=new ArrayList();
				sendertuple.add(format[1]);
				sendertuple.add(Integer.parseInt(format[2]));
				Float costOriginal=linkDownRecord.get(sendertuple);
				nearbyCostList.put(sendertuple, costOriginal);
				Neighbors ng=new Neighbors(format[1], (int)sendertuple.get(1), sendertuple);
				neighborClient.add(ng);

			}
			
		}

	}

}