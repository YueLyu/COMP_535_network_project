package socs.network.node;

import socs.network.message.SOSPFPacket;
import socs.network.util.Configuration;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Router {

	protected LinkStateDatabase lsd;

	RouterDescription rd = new RouterDescription();

	// assuming that all routers are with 4 ports
	Link[] ports = new Link[4];
	private ServerSocket serverSocket;

	public Router(Configuration config) {
		rd.processIPAddress = "127.0.0.1";
		rd.simulatedIPAddress = config.getString("socs.network.router.ip");
		rd.processPortNumber = (short) Integer.parseInt(config.getString("socs.network.router.port"));
		rd.status = null;
		// lsd = new LinkStateDatabase(rd);
		// Start the server.
		try {
			serverSocket = new ServerSocket(rd.processPortNumber);
			System.out.println("ServerSocket start!");
		} catch (IOException e) {
			e.printStackTrace();
		}
		Thread thread = new Thread(new Server(this.serverSocket, this.ports, this.rd));
		thread.start();
	}

	/**
	 * output the shortest path to the given destination ip
	 * <p/>
	 * format: source ip address -> ip address -> ... -> destination ip
	 *
	 * @param destinationIP the ip adderss of the destination simulated router
	 */
	private void processDetect(String destinationIP) {

	}

	/**
	 * disconnect with the router identified by the given destination ip address
	 * Notice: this command should trigger the synchronization of database
	 *
	 * @param portNumber the port number which the link attaches at
	 */
	private void processDisconnect(short portNumber) {

	}

	/**
	 * attach the link to the remote router, which is identified by the given
	 * simulated ip; to establish the connection via socket, you need to indentify
	 * the process IP and process Port; additionally, weight is the cost to
	 * transmitting data through the link
	 * <p/>
	 * NOTE: this command should not trigger link database synchronization
	 */
	private void processAttach(String processIP, short processPort, String simulatedIP, short weight) {
		for (Link link : this.ports) {
			if (link != null) {
				if (link.router2.simulatedIPAddress.equals(simulatedIP)
						&& link.router2.processPortNumber == processPort) {
					System.out.println("Already attached to IP: " + link.router2.simulatedIPAddress);
					return;
				}
			}
		}
		int toAdd = -1;
		for (int i = 0; i < this.ports.length; i++) {
			if (this.ports[i] == null) {
				toAdd = i;
				break;
			}
		}
		// Check if ports array is full.
		if (toAdd == -1) {
			System.out.print("All 4 ports of the router are occupied. Can not process.");
			return;
		}
		RouterDescription neighbor = new RouterDescription();
		neighbor.processIPAddress = processIP;
		neighbor.processPortNumber = processPort;
		neighbor.simulatedIPAddress = simulatedIP;
		this.ports[toAdd] = new Link(rd, neighbor, weight);
	}

	/**
	 * broadcast Hello to neighbors
	 */
	private void processStart() {
		Link[] ports_cpy = new Link[4];
		int pos = 0;
		// Send message to neighbors as client.
		for (Link link : this.ports) {

			if (link == null) {
				continue;
			}
			if (link.router2.status != null && link.router2.status.equals(RouterStatus.TWO_WAY)) {
				System.out.println(
						"Two way communication with " + link.router2.simulatedIPAddress + " already established.");
				System.out.println("No packet sent.");
				ports_cpy[pos] = link;
				pos++;
				continue;
			}

			try {
				SOSPFPacket clientPacket = new SOSPFPacket();
				clientPacket.srcProcessIP = rd.processIPAddress;
				clientPacket.srcProcessPort = rd.processPortNumber;
				clientPacket.srcIP = rd.simulatedIPAddress;
				clientPacket.dstIP = link.router2.simulatedIPAddress;
				clientPacket.sospfType = 0;
				clientPacket.routerID = rd.simulatedIPAddress;
				clientPacket.neighborID = link.router2.simulatedIPAddress;
				clientPacket.weight = link.weight;

				System.out.println("Connecting to " + link.router2.simulatedIPAddress + " on port "
						+ link.router2.processPortNumber);
				Socket clientSocket = new Socket(link.router2.processIPAddress, link.router2.processPortNumber);
				System.out.println("Connect Complete!");

				// write to server
				ObjectOutputStream outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
				outputStream.writeObject(clientPacket);

				// read from server
				ObjectInputStream inputStream = new ObjectInputStream(clientSocket.getInputStream());
				SOSPFPacket serverPacket = (SOSPFPacket) inputStream.readObject();

				if (serverPacket.sospfType == -1) {
					System.out.println("Link with " + link.router2.simulatedIPAddress
							+ " failed because all 4 ports of the target router have been occupied.");
					System.out.println("Link with target router deleted.");
					continue;
				}

				if (serverPacket.sospfType == 0) {
					System.out.println("Client Received Hello from " + serverPacket.srcIP);
					link.router2.status = RouterStatus.TWO_WAY;
					System.out.println(
							"set " + link.router2.simulatedIPAddress + " state to " + link.router2.status + ";");
				}

				// send again
				outputStream.writeObject(clientPacket);
				clientSocket.close();
				ports_cpy[pos] = link;
				pos++;
			} catch (ClassNotFoundException | IOException e) {
				e.printStackTrace();
			}
		}
		for (int i = 0; i < ports_cpy.length; i ++) {
			this.ports[i] = ports_cpy[i];
		}
	}

	/**
	 * attach the link to the remote router, which is identified by the given
	 * simulated ip; to establish the connection via socket, you need to indentify
	 * the process IP and process Port; additionally, weight is the cost to
	 * transmitting data through the link
	 * <p/>
	 * This command does trigger the link database synchronization
	 */
	private void processConnect(String processIP, short processPort, String simulatedIP, short weight) {

	}

	/**
	 * output the neighbors of the routers
	 */
	private void processNeighbors() {
		for (int i = 0; i < this.ports.length; i++) {
			if (this.ports[i] != null) {
				System.out.println("IP Address of the neighbor " + i + ": " + this.ports[i].router2.simulatedIPAddress);
			}
		}
	}

	/**
	 * disconnect with all neighbors and quit the program
	 */
	private void processQuit() {

	}

	public void terminal() {
		try {
			InputStreamReader isReader = new InputStreamReader(System.in);
			BufferedReader br = new BufferedReader(isReader);
			System.out.print(">> ");
			String command = br.readLine();
			while (true) {
				if (command.startsWith("detect ")) {
					String[] cmdLine = command.split(" ");
					processDetect(cmdLine[1]);
				} else if (command.startsWith("disconnect ")) {
					String[] cmdLine = command.split(" ");
					processDisconnect(Short.parseShort(cmdLine[1]));
				} else if (command.startsWith("quit")) {
					processQuit();
				} else if (command.startsWith("attach ")) {
					String[] cmdLine = command.split(" ");
					processAttach(cmdLine[1], Short.parseShort(cmdLine[2]), cmdLine[3], Short.parseShort(cmdLine[4]));
				} else if (command.equals("start")) {
					processStart();
				} else if (command.equals("connect ")) {
					String[] cmdLine = command.split(" ");
					processConnect(cmdLine[1], Short.parseShort(cmdLine[2]), cmdLine[3], Short.parseShort(cmdLine[4]));
				} else if (command.equals("neighbors")) {
					// output neighbors
					processNeighbors();
				} else {
					// invalid command
					break;
				}
				System.out.print(">> ");
				command = br.readLine();
			}
			isReader.close();
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
