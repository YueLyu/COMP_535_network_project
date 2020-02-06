package socs.network.node;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import socs.network.message.SOSPFPacket;

public class Server implements Runnable {
	class Channel implements Runnable {
		ObjectOutputStream outputStream;
		ObjectInputStream inputStream;
		Channel(ObjectOutputStream outputStream, ObjectInputStream inputStream) {
			this.outputStream = outputStream;
			this.inputStream = inputStream;
		}

		@Override
		public void run() {
			while (true) {
				try {
					SOSPFPacket receivedPacket = (SOSPFPacket) inputStream.readObject();

					RouterDescription clientRouter = findClient(receivedPacket.srcIP);
					// verify attachment
					if (!receivedPacket.dstIP.equals(rd.simulatedIPAddress)) {
						System.out.print("No link exists");
					}
					if (clientRouter == null) {
						int toAdd = firstEmptyPort();
						if (toAdd != -1) {
							clientRouter = new RouterDescription();
							clientRouter.processIPAddress = receivedPacket.srcProcessIP;
							clientRouter.processPortNumber = receivedPacket.srcProcessPort;
							clientRouter.simulatedIPAddress = receivedPacket.srcIP;
							ports[toAdd] = new Link(rd, clientRouter, receivedPacket.weight);
						} else {
							System.out.println("All ports have been occupied");
							SOSPFPacket responsePacket = new SOSPFPacket();
							responsePacket.sospfType = -1;
							outputStream.writeObject(responsePacket);
							return;
						}
					}

					// First receive HELLO, set clientRouter.status to INIT.
					if (receivedPacket.sospfType == 0 && clientRouter.status == null) {
						clientRouter.status = RouterStatus.INIT;
						System.out.println("Server Received HELLO from " + receivedPacket.srcIP + ";");
						System.out.println(
								"set " + clientRouter.simulatedIPAddress + " state to " + clientRouter.status + ";");

						// send response packet
						SOSPFPacket responsePacket = new SOSPFPacket();
						responsePacket.srcProcessIP = rd.processIPAddress;
						responsePacket.srcProcessPort = rd.processPortNumber;
						responsePacket.srcIP = rd.simulatedIPAddress;
						responsePacket.dstIP = clientRouter.simulatedIPAddress;
						responsePacket.sospfType = 0;
						responsePacket.routerID = clientRouter.simulatedIPAddress;
						responsePacket.neighborID = rd.simulatedIPAddress;
						short weight = 0;
						for (Link link : ports) {
							if (link.router2 == clientRouter) {
								weight = link.weight;
								break;
							}
						}
						responsePacket.weight = weight;

						outputStream.writeObject(responsePacket);
					} else if (receivedPacket.sospfType == 0 && clientRouter.status == RouterStatus.INIT) { 
						clientRouter.status = RouterStatus.TWO_WAY;
						System.out.println("Server Received HELLO from " + receivedPacket.srcIP + ";");
						System.out.println(
								"set " + clientRouter.simulatedIPAddress + " state to " + clientRouter.status + ";");
						return;
					} else if (receivedPacket.sospfType == 1) {
						// LSA update
					}
				} catch (SocketException e) {
					System.out.println("Client closed, server thread stops.");
					return;
				} catch (EOFException e) {
					// Ignore.
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

	}

	private ServerSocket serverSocket;
	private Link[] ports;
	private RouterDescription rd;

	public Server(ServerSocket serverSocket, Link[] ports, RouterDescription rd) {
		this.serverSocket = serverSocket;
		this.ports = ports;
		this.rd = rd;
	}

	private RouterDescription findClient(String sip) {
		for (int i = 0; i < this.ports.length; i++) {
			if (this.ports[i] != null && this.ports[i].router2.simulatedIPAddress.equals(sip)) {
				return this.ports[i].router2;
			}
		}
		return null;
	}

	private int firstEmptyPort() {
		for (int i = 0; i < this.ports.length; i++) {
			if (this.ports[i] == null) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public void run() {
		while (true) {
            try {
                Socket clientSocketHandle = serverSocket.accept();
                System.out.println("Client Connected!");
                // write to client
                ObjectOutputStream outputStream = new ObjectOutputStream(clientSocketHandle.getOutputStream());
                // read from client
                ObjectInputStream inputStream = new ObjectInputStream(clientSocketHandle.getInputStream());
                new Thread(new Channel(outputStream, inputStream)).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
	}
}
