**********************************************
        Name: Yaxin Wang
        Email: yw2770@columbia.edu
**********************************************
     Demo: https://youtu.be/mjOaQmMXNmw
   

**********************************************
	Development Enviroment
**********************************************
Java version Message:
  * java version "1.8.0_60”;
  * Java(TM) SE Runtime Environment (build 1.8.0_60-b27);
  * Java HotSpot(TM) 64-Bit Server VM (build 25.60-b23, mixed mode);


***********************************************
        Functions realized
***********************************************
1, Nodes creation in the network;
2, SHOWRT to show the distance vector table of this node, in the format:Destination = ip:port, Cost = #, NearbyCost: #, (linknode ip :linknode port);
3, LINKDOWN to link down an edge in the network;
4, LINKUP to recover an edge which has been LINKDOWN before;
5, CLOSE a node in the network;


***********************************************
        How to compile and run
***********************************************
1, In terminal, cd to the folder of my assignment;
2, Type “make” or type “javac *.java” in terminal to compile;

3, To run Client, type “java Client <localport> <timeout> <neighbor ipaddress1> <neighbor port1> <weight1> <neighbor ipaddress2> <neighbor ipaddress2> <neighbor port2> <weight2>; 
   * Neighbor should be the node which has already created in the network, or it may cause some problems;
   * Ipaddress cannot be “localhost”, your ip will be printed in terminal after the nodes created, please use the #.#.#.# format ip;
   * For example, first create node by type ”java Client 4115 3”; Second node created by type “java Client 4116 3 192.168.22.86 4115 5.0”; Third node created by type “java Client 4118 3 192.168.22.86 4115 30.0 192.168.22.86 4116 5.0”;Fourth node created by “java Client 4117 3 192.168.22.86 4116 10.0”;

4, To show DV table in local node, type “SHOWRT” in terminal;
   * DV table printed in terminal of format: Destination = ip:port, Cost = #, NearbyCost:#, (linknode ip :linknode port);

5, To destroy an edge in network, type “LINKDOWN <ipaddress> <port>” in terminal;
   * If we want to destroy the edge (A, B), we need to LINKDOWN in node A or B and the ipaddress and the port are of the other node in this edge;
   * For example, in the network we construct in 3, if we want to LINKDOWN the edge (4115, 4116), we can type “LINKDOWN 192.168.22.86 4116” in terminal of node with 4115 port, or type “LINKDOWN 192.168.22.86 4115” in the terminal of node with 4116 port;

6, To recover an edge which has been LINKDOWN before, type “LINKUP <ip> <port>” in terminal；
   * If we want to recovery the edge (A, B), we need to LINKUP in node A or B and the ipaddress and the port are of the other node in this edge;
   * For example, in the example network, if we want to recovery the edge (4115, 4116) we can type “LINKUP 192.168.22.86 4115” in the terminal of node with 4116 port or type “LINKUP 192.168.22.86 4116” in terminal of node with 4115 port;
7, To close a node in the network, type “CLOSE” in terminal;
   * If we want to close node A, we need to type “CLOSE” in this node terminal;
   * For example, if we want to close node with port 4118 in the example node, type “CLOSE” in terminal which run this 4118 port node;

Thanks a lot.
