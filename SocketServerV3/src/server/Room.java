package server;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;

public class Room implements AutoCloseable {
	private static SocketServer server;// used to refer to accessible server functions
	private String name;
	private final static Logger log = Logger.getLogger(Room.class.getName());

	// Commands
	private final static String COMMAND_TRIGGER = "/";
	private final static String CREATE_ROOM = "createroom";
	private final static String JOIN_ROOM = "joinroom";

	public Room(String name) {
		this.name = name;
	}

	public static void setServer(SocketServer server) {
		Room.server = server;
	}

	public String getName() {
		return name;
	}

	private List<ServerThread> clients = new ArrayList<ServerThread>();

	protected synchronized void addClient(ServerThread client) {
		client.setCurrentRoom(this);
		if (clients.indexOf(client) > -1) {
			log.log(Level.INFO, "Attempting to add a client that already exists");
		} else {
			clients.add(client);
			if (client.getClientName() != null) {
				client.sendClearList();
				sendConnectionStatus(client, true, "joined the room " + getName());
				updateClientList(client);
			}
		}
	}

	private synchronized void updateClientList(ServerThread client) {
		Iterator<ServerThread> iter = clients.iterator();
		while (iter.hasNext()) {
			ServerThread c = iter.next();
			if (c != client) {
				boolean messageSent = client.sendConnectionStatus(c.getClientName(), true, null);
			}
		}
	}

	protected synchronized void removeClient(ServerThread client) {
		Iterator<ServerThread> iter = clients.iterator();
		while (iter.hasNext()) {
			ServerThread c = iter.next();
			if (c == client) {
				iter.remove();
				log.log(Level.INFO, "Removed client " + c.getClientName() + " from " + getName());
			}
		}
		if (clients.size() > 0) {
			sendConnectionStatus(client, false, "left the room " + getName());
		} else {
			cleanupEmptyRoom();
		}

		/*
		 * clients.remove(client); if (clients.size() > 0) { // sendMessage(client,
		 * "left the room"); sendConnectionStatus(client, false, "left the room " +
		 * getName()); } else { cleanupEmptyRoom(); }
		 */
	}

	private void cleanupEmptyRoom() {
		// If name is null it's already been closed. And don't close the Lobby
		if (name == null || name.equalsIgnoreCase(SocketServer.LOBBY)) {
			return;
		}
		try {
			log.log(Level.INFO, "Closing empty room: " + name);
			close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected void joinRoom(String room, ServerThread client) {
		server.joinRoom(room, client);
	}

	protected void joinLobby(ServerThread client) {
		server.joinLobby(client);
	}

	protected void createRoom(String room, ServerThread client) {
		if (server.createNewRoom(room)) {
			sendMessage(client, "Created a new room");
			joinRoom(room, client);
		}
	}

	/***
	 * Helper function to process messages to trigger different functionality.
	 * 
	 * @param message The original message being sent
	 * @param client  The sender of the message (since they'll be the ones
	 *                triggering the actions)
	 */
	private String processCommands(String message, ServerThread client) {
		String response = null;
		try {
			if (message.indexOf(COMMAND_TRIGGER) > -1) {
				String[] comm = message.split(COMMAND_TRIGGER);
				log.log(Level.INFO, message);
				String part1 = comm[1];
				String[] comm2 = part1.split(" ");
				String command = comm2[0];
				if (command != null) {
					command = command.toLowerCase();
				}
				String roomName;
				switch (command) {
				case CREATE_ROOM:
					roomName = comm2[1];
					if (server.createNewRoom(roomName)) {
						joinRoom(roomName, client);
					}
					break;
				case JOIN_ROOM:
					roomName = comm2[1];
					joinRoom(roomName, client);
					break;

				case "roll": // rolls die
					String num = Integer.toString((int) ((Math.random() * 6) + 1));
					response = "<font color =blue> ⚄ : " + num + "</font>";
					break;
				case "flip": // rolls heads or tails
					int ranflip = (int) (Math.random() * 2);
					if (ranflip == 0) {
						response = "<font color = blue> ◌: heads </font>";
					} else {
						response = "<font color = blue> ◌: tails </font>";
					}
					break;
				case "pm":
					response = null;
					String[] pm = message.split("@"); // last split will be @lastusername + message
					int size = pm.length;
					List<String> users = new ArrayList<String>();
					String nStr = pm[size - 1]; // should be last username + message
					String[] nMess = nStr.split(" ", 2);
					String m1 = nMess[1]; // this is our message to be sent to the target user

					for (int i = 1; i < size - 1; i++) {
						users.add(pm[i].trim());
					}
					users.add(nMess[0]);
					sendPrivateMessage(client, m1, users);
					break;
				case "mute": // get this specific user to add or remove to mute list
					response = null;
					String[] mu = message.split("@"); // last split will be @lastusername + message
					int mSize = mu.length;
					List<String> usersM = new ArrayList<String>();
					String nStrM = mu[mSize - 1]; // should be last username + message
					String[] nMessM = nStrM.split(" ", 2);

					for (int i = 1; i < mSize - 1; i++) {
						usersM.add(mu[i].trim());
					}
					usersM.add(nMessM[0]); // user list is now complete

					// adds client to sender mute list
					for (int i = 0; i < usersM.size(); i++) {
						System.out.println(usersM.get(i));
						if (!client.mutedClients.contains(usersM.get(i))) {
							client.mutedClients.add(usersM.get(i));
							client.sendMuteStatus(usersM.get(i), true);
						}
						sendPrivateMessage(client, "You have been muted", usersM);
					}

					break;
				case "unmute":
					response = null;
					String[] un = message.split("@"); // last split will be @lastusername + message
					int unSize = un.length;
					List<String> usersUn = new ArrayList<String>();
					String nStrUn = un[unSize - 1]; // should be last username + message
					String[] nMessUn = nStrUn.split(" ", 2);

					for (int i = 1; i < unSize - 1; i++) {
						usersUn.add(un[i].trim());
					}
					usersUn.add(nMessUn[0]); // user list is now complete

					// removes client from sender mute list
					for (int i = 0; i < usersUn.size(); i++) {
						if (client.mutedClients.contains(usersUn.get(i))) {
							client.mutedClients.remove(usersUn.get(i));
							client.sendMuteStatus(usersUn.get(i), false);
						}
					}
					break;

				default:
					// not a command, let's fix this function from eating messages
					response = message;
					break;
				}
			} else {
				String nMess = message;

				if (StringUtils.countMatches(nMess, "*") > 1) { // bold
					String[] s = nMess.split("\\*");
					String m = "";
					m += s[0];
					for (int i = 1; i < s.length; i++) {
						if (i % 2 == 0) {
							m += s[i];
						} else {
							m += "<b>" + s[i] + "</b>";
						}
						System.out.println(s[i]);
					}
					nMess = m;
				}

				if (StringUtils.countMatches(nMess, "_") > 1) { // underline
					String[] s = nMess.split("\\_");
					String m = "";
					m += s[0];
					for (int i = 1; i < s.length; i++) {
						if (i % 2 == 0) {
							m += s[i];
						} else {
							m += "<u>" + s[i] + "</u>";
						}
						System.out.println(s[i]);
					}
					nMess = m;
				}

				if (StringUtils.countMatches(nMess, "~") > 1) { // italics
					String[] s = nMess.split("\\~");
					String m = "";
					m += s[0];
					for (int i = 1; i < s.length; i++) {
						if (i % 2 == 0) {
							m += s[i];
						} else {
							m += "<i>" + s[i] + "</i>";
						}
						System.out.println(s[i]);
					}
					nMess = m;
				}

				if (StringUtils.countMatches(nMess, "#") > 1) { // changes font to red
					String[] s = nMess.split("\\#");
					String m = "";
					m += s[0];
					for (int i = 1; i < s.length; i++) {
						if (i % 2 == 0) {
							m += s[i];
						} else {
							m += "<font color=red>" + s[i] + "</font>";
						}
						System.out.println(s[i]);
					}
					nMess = m;
				}

				response = nMess;

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		// return wasCommand;
		return response;
	}

	// TODO changed from string to ServerThread
	protected void sendConnectionStatus(ServerThread client, boolean isConnect, String message) {
		Iterator<ServerThread> iter = clients.iterator();
		while (iter.hasNext()) {
			ServerThread c = iter.next();
			boolean messageSent = c.sendConnectionStatus(client.getClientName(), isConnect, message);
			if (!messageSent) {
				iter.remove();
				log.log(Level.INFO, "Removed client " + c.getId());
			}
		}
	}

	/***
	 * Takes a sender and a message and broadcasts the message to all clients in
	 * this room. Client is mostly passed for command purposes but we can also use
	 * it to extract other client info.
	 * 
	 * @param sender  The client sending the message
	 * @param message The message to broadcast inside the room
	 */
	protected void sendMessage(ServerThread sender, String message) {
		log.log(Level.INFO, getName() + ": Sending message to " + clients.size() + " clients");
		String resp = processCommands(message, sender);
		if (resp == null) {
			// it was a command, don't broadcast
			return;
		}
		message = resp;

		/*
		 * if (processCommands(message, sender)) { // it was a command, don't broadcast
		 * return; }
		 */

		Iterator<ServerThread> iter = clients.iterator();
		while (iter.hasNext()) {
			ServerThread client = iter.next();
			if (!client.isMuted(sender.getClientName())) {
				boolean messageSent = client.send(sender.getClientName(), message);
				if (!messageSent) {
					iter.remove();
					log.log(Level.INFO, "Removed client " + client.getId());
				}
			}
		}

	}

	protected void sendPrivateMessage(ServerThread sender, String message, List<String> users) {
		Iterator<ServerThread> iter = clients.iterator();
		while (iter.hasNext()) {
			ServerThread client = iter.next();
			if (users.contains(client.getClientName()) || client.getClientName().equals(sender.getClientName())) {
				boolean messageSent = client.send(sender.getClientName(), message);
				if (!messageSent) {
					iter.remove();
					log.log(Level.INFO, "Removed client " + client.getId());
				}
			}
		}

	}

	public List<String> getRooms(String search) {
		return server.getRooms(search);
	}

	/***
	 * Will attempt to migrate any remaining clients to the Lobby room. Will then
	 * set references to null and should be eligible for garbage collection
	 */
	@Override
	public void close() throws Exception {
		int clientCount = clients.size();
		if (clientCount > 0) {
			log.log(Level.INFO, "Migrating " + clients.size() + " to Lobby");
			Iterator<ServerThread> iter = clients.iterator();
			Room lobby = server.getLobby();
			while (iter.hasNext()) {
				ServerThread client = iter.next();
				lobby.addClient(client);
				iter.remove();
			}
			log.log(Level.INFO, "Done Migrating " + clients.size() + " to Lobby");
		}
		server.cleanupRoom(this);
		name = null;
		// should be eligible for garbage collection now
	}

}