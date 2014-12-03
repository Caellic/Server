import java.net.*;
import java.io.*;

public class ServerThread extends Thread {
	/** Data Members */
	private Server server = null;
	private Socket socket = null;
	private Database db = new Database();
	private ObjectOutputStream oos;
	String msg;
	User usr;
	GameObj game;
	
	/** Server Thread Constructor */
	public ServerThread(Server server, Socket socket, ObjectOutputStream oos) {
		super("ServerThread");
		this.server = server;
		this.socket = socket;
		this.oos = oos; 
		start(); 
		db.InitializeDB(); 
	}

	/***********************************************************
	 *				  Start Receiving Messages				   *
	 ***********************************************************/
	public void run() {
		try {
			ObjectInputStream ois = 
				new ObjectInputStream(socket.getInputStream());						
			
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
						case "MOVEMADE":
							onMoveMade(input);
							break;
						case "CHAT":
							onChat(input);
							break;
						case "QUITGAME":
							onQuitGame();
							break;
						case "LOGOUT":
							onLogout();
							break;
						case "VALIDATEREGISTRATION":
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
				server.removeConnection( socket, this );
		}
	}
	
	
	/***********************************************************
	 *				  		Login Code						   *
	 ***********************************************************/
	public void onLogin(Message input){
		try{
			usr = (User)(input.MessageData);							
			System.out.println("User: " + 
					usr.username + " | Pass: " +
					usr.password);
			
			boolean isValidLogin = 
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
				if(isValidLogin){
					server.isDuplicate(usr.username, this);
					server.loggedIn(usr.username);					
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
	
	/***********************************************************
	 *				  		Game Code						   *
	 ***********************************************************/
	public void onStartGame(){
		server.setUpNewGame(usr.username, this);
		server.sendlisttoyou(usr.username);	
		System.out.println(usr.username + " has started a game.");
		SendMessage(game, "STARTEDGAME");
	}

	public void onMoveMade(Message input){
		String msg = (String) input.MessageData;
		String[] rowCol = msg.split(", ");
		int row = Integer.parseInt(rowCol[0]);
		int col = Integer.parseInt(rowCol[1]);
		server.updatePlayerMove(this, row, col);
	}
	
	public void onJoinGame(){
		server.findGameToJoin(usr.username, this);
	}
	
	public void onQuitGame(){
		server.quitGame(this);
		server.updateUserList(usr.username);
	}
	
	/***********************************************************
	 *					 	Chat Code						   *
	 ***********************************************************/
	public void onChat(Message input){
		String msg = (String) input.MessageData;
		server.sendChats(usr.username, msg);
	}
	
	/***********************************************************
	 *						Logout Code						   *
	 ***********************************************************/
	public void onLogout(){
		server.loggedOut(usr.username);
		server.updateUserList(usr.username);
	}

	/***********************************************************
	 *					Registering Code					   *
	 ***********************************************************/
	public void onValidateUserRegistration(Message input){
		// Get user and check it against the database
		try{
			String user = (String)(input.MessageData);		
			boolean isRegistrationValid = 
					db.executeValidateRegistrationQuery(user);
			
			try {
				if(isRegistrationValid){
					// Username is free to register
					SendMessage(new String(), "RGSTERUSERPASSED");
				}
				else{
					// Username is not free to use
					SendMessage(new String(), "RGSTERUSERFAILED");									
				}
			} catch (Exception ex){
				ex.printStackTrace();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public void onRegister(Message input){
		// Registration has passed, register user and pass
		try{
			usr = (User)(input.MessageData);							
			System.out.println("Registered User: " + 
					usr.username + " | Pass: " +
					usr.password);			
			db.executeRegistrationQuery(usr.username, usr.password);	

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	/***********************************************************
	 *		Send Message from Thread to Specific Client		   *
	 ***********************************************************/
	public void SendMessage(Object obj, String messageType){		
		Message msg = new Message();
		msg.MessageType = messageType;
		msg.MessageData = obj;

		try{
			oos.writeObject(msg);
			oos.flush();
			oos.reset();
		} catch(Exception ex){
			ex.printStackTrace();
		}
	}
}
