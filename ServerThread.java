import java.net.*;
import java.io.*;

public class ServerThread extends Thread {
	private Server server = null;
	private Socket socket = null;
	private Database db = new Database();
	String msg;
	User usr;
	GameObj game;
	
//	private PrintWriter out;
	private ObjectOutputStream oos;

	public ServerThread(Server server, Socket socket, ObjectOutputStream oos) {
		super("ServerThread");
		this.server = server;
		this.socket = socket;
		this.oos = oos;
		start();
		db.InitializeDB();
	}

	public void run() {
		try {
			ObjectInputStream ois = 
				new ObjectInputStream(socket.getInputStream());			
			//oos = new ObjectOutputStream(socket.getOutputStream());
			Message input = null;

			while (true) {
				try {
					input = (Message) ois.readObject();
					
				} catch (Exception ex){
					System.out.println(socket);
					ex.printStackTrace();
					System.out.println("We broke, hi.");
					break;
				}

				if (input != null) {

					switch (input.MessageType) {
						case "LOGIN": 
							onLogin(input);
							break;
						case "STARTGAME":
							onStartGame();
							break;
						case "JOINGAME":
							onJoinGame();
							break;
						case "CHAT":
							onChat(input);
							break;
						case "LOGOUT":
							onLogout();
							break;
						case "LOGOUTUSER":
							onLogoutUser();
							break;
						case "REGISTERUSER":
							onValidateUserRegistration(input);
							break;
						case "REGISTER":
							onRegister(input);
							break;
						default:
							break;
					}
				}
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		} finally {
				server.removeConnection( socket );				
		}
	}
	
	/** When user logs in, run this code */
	public void onLogin(Message input){
		try{
			usr = (User)(input.MessageData);							
			System.out.println("User: " + 
					usr.username + " | Pass: " +
					usr.password);
			db.executeLoginQuery(usr.username, usr.password);	
			try {
				// Just to by-pass needing the database to login -- For Prototype Stuff
				/*if(usr.username.toLowerCase()
						.equals("test")
						&& usr.password.toLowerCase()
						.equals("test")){
					SendMessage(new String(), "PASSED");
				}
				else{
					SendMessage(new String(), "FAILED");	
				}
				*/
				System.out.println(db.getValid());
				if(db.getValid()){
				//	server.sendlisttoyou(usr.username);	
					server.loggedIn(usr.username);
					server.isDuplicate(usr.username);
				//	server.sendToAll(usr.username, "-ENTERED ");
					
				}
				else{
					server.sendToAll(new String(), "FAILED");
					
				}
			} catch (Exception ex){
				ex.printStackTrace();
			}
			
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	/** Start Game Code */
	public void onStartGame(){
		server.setUpNewGame(usr.username, this);
		server.sendlisttoyou(usr.username);	
		System.out.println(usr.username + " has started a game.");
		SendMessage(game, "STARTEDGAME");
	}
	
	/** Start Game Code */
	public void onUpdateMovesGame(){
		// Wait
	}
	
	/** Start Game Code */
	public void onJoinGame(){
		server.setUpJoinGame(usr.username, this);
	}
	
	/** Chat Code */
	public void onChat(Message input){
		String msg = (String) input.MessageData;
		server.sendChats(usr.username, msg);
	}
	
	/** Logout Code */
	public void onLogout(){
		server.updateUserList(usr.username);
	}
	
	/** Logout User Code */
	public void onLogoutUser(){
		server.loggedOut(usr.username);
	}
	
	/** Validate Registered User Code */
	public void onValidateUserRegistration(Message input){
		try{
			String user = (String)(input.MessageData);							
			System.out.println(user);
			db.executeValidateRegistrationQuery(user);	
			try {
				System.out.println(db.getValid());
				if(db.getValid()){
					SendMessage(new String(), "USERPASSED");
				}
				else{
					SendMessage(new String(), "USERFAILED");									
				}
			} catch (Exception ex){
				ex.printStackTrace();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	/** Register User Code */
	public void onRegister(Message input){
		try{
			usr = (User)(input.MessageData);							
			System.out.println("User: " + 
					usr.username + " | Pass: " +
					usr.password);
			db.executeRegistrationQuery(usr.username, usr.password);	

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	public void SendMessage(Object obj, String messageType){		
		Message msg = new Message();
		msg.MessageType = messageType;
		msg.MessageData = obj;

		try{
			oos.writeObject(msg);
			oos.flush();
		} catch(Exception ex){
			ex.printStackTrace();
		}
	}
	
/*
	public void SendMessage(String message){
		out.println(message);
		out.flush();
	}*/

	/*
	public void Close(){
		try {
//			out.close();
			socket.close();
		} catch(Exception ex){
			ex.printStackTrace();
		}
	}*/
}
