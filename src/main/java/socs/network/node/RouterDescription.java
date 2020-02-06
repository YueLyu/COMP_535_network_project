package socs.network.node;

public class RouterDescription {
  //used to socket communication
  String processIPAddress;
  short processPortNumber;
  //used to identify the router in the simulated network space
  String simulatedIPAddress;
  //status of the router
  RouterStatus status;
  
  // Default constructor
  public RouterDescription() {
	  
  }
  
  // Copy constructor
  public RouterDescription(RouterDescription rd) {
	  this.processIPAddress = rd.processIPAddress;
	  this.processPortNumber = rd.processPortNumber;
	  this.simulatedIPAddress = rd.simulatedIPAddress;
	  this.status = rd.status;
  }
}
