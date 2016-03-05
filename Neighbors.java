import java.net.*;
import java.util.*;

public class Neighbors{
	
	String ipaddress;
	int port;
	List link;
	List localMsg=new ArrayList();
	
	public Neighbors(String ip, int pt, List l){
		ipaddress=ip;
		port=pt;
		link=l;
		localMsg.add(ip);
		localMsg.add(pt);
	}

	public int getPort(){
		return port;
	}

	public String getIP(){
		return ipaddress;
	}

	public List getLinkMsg(){
		return link;
	}

	public void setLinkMsg(List l){
		this.link=l;
	}
	public List getLocalMsg(){
		return localMsg;
	}

	
}