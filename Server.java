import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

public class Server {

	public static void main(String[] args){
		System.out.println("Server Starting");
		
		Server foo = new Server(40000);
	}

	public Server(int port){
		System.out.println("Initializing server.");
				
		try{
			ServerSocket serverSocket = new ServerSocket(port);
			
			// Tell the world we're ready to go
			System.out.println( "Listening on "+ serverSocket );			
			
			while(true){
				Socket communicationSocket = serverSocket.accept();
				
				// Tell the world we've got it
				System.out.println( "Connection from "+ communicationSocket );
				
				// Create a DataOutputStream for writing data to the
				// other side
				ObjectOutputStream oout = new ObjectOutputStream( communicationSocket.getOutputStream() );
				// Save this stream so we don't need to make it again
				outputStreams.put( communicationSocket, oout );
				// Create a new thread for this connection, and then forget
				// about it
				threads.add( new ServerThread( this, communicationSocket, oout ) );

			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	/** Method to check if username is already logged in */
	public void isDuplicate(String username) {
		int count = 0;
		for(String u: names){
			if(u.equals(username)){			
				count++;
			}
		}
		if(count <= 1){
			sendToAll(new String(), "PASSED");
		}
		else{
			sendToAll(new String(), "DUPLICATELOGIN");
		}
    }
	
	/** Method add username to names when user name is logged in */
	public void loggedIn(String username) {
		names.add(username);
	}
	
	/** Method remove username from names when user name is logged out */
	public void loggedOut(String username) {
		names.remove(username);
	}
	
	/** Method count username in names - Only send username list if
	 *  username is not already there */
	public void sendlisttoyou(String username) {
		int count = 0;
		for(String u: names){
			if(u.equals(username)){					
				System.out.println(u);
				count++;
				System.out.println(count);
			}
		}
		if(count <= 1){
	        name += username + "\n";
	        sendToAll(name, "-USERNAMES ");
	        sendToAll(username, "-ENTERED ");
		}
    }
	
	/** Method to update user list  */
	public void updateUserList(String username) {
		int count = 0;
		for(String u: names){
			if(u.equals(username)){			
				count++;
			}
			System.out.println("Count of usernames: " + count);
		}
		
		names.remove(username);
		
		if(name.contains(username) && count <= 1){
			name = name.replace(username + "\n", "");
	        sendToAll(username, "-EXITED ");
	        sendToAll(name, "-USERNAMES ");

		}		
        System.out.println(name);
    }
    
	/** Method to send chat messages */
	public void sendChats(String username, String msg){
		chatMsg = msg + "\n";
		sendToAll(new String(), username + " " + chatMsg);
	}
	
	/** Method set up a new game */
	public void setUpNewGame(String username, ServerThread curThread){
		games.put(username, new GameObj(username));
		curThread.game = games.get(username);
		System.out.println(games.get(username) + " was created.");
		
		for(ServerThread u: threads){
			if(u.usr.username.equals(username)){
				System.out.println("Hey");
			}
		}
	}
	
	/** Method for joining a game */
	public void setUpJoinGame(String username, ServerThread curThread){
		boolean joinedGameSuccess = false;
		System.out.println("I'm in this method.");
	    
		for (Enumeration<GameObj> e = games.elements(); e.hasMoreElements(); ) {
			GameObj game = (GameObj)e.nextElement();
			if(game.getPlayer2() == null){
				System.out.println("Player2 for this game is in fact null.");
					if(curThread.usr.username.equals(username)){
						System.out.println("The user is " + username);
						curThread.game = game;
						curThread.game.setPlayer1(game.getPlayer1());
						curThread.game.setPlayer2(username);
						curThread.SendMessage(game, "STARTEDGAME");
						sendlisttoyou(username);
						System.out.println("The user " + username + " has joined the game: " + game);
						joinedGameSuccess = true;
						break;
				}
			}
		}
	    
		if(!joinedGameSuccess){
			curThread.SendMessage(new String(), "JOINFAILED");
			System.out.println("Joining a game failed.");
		}
		else{
			for(ServerThread m: threads){
				if(m.usr.username.equals(curThread.game.getPlayer1())){
					System.out.println(m.usr.username + " the user name.");
					m.game = curThread.game;
				}
			}				
			
			updateGame(curThread);	

		}
	}
	
	public void updateGame(ServerThread player){
		ServerThread player1;
		ServerThread player2;
			for(ServerThread u: threads){
				if(u.usr.username.equals(player.game.getPlayer1())){
					player1 = u;
					Player2JoinedGame(player1);

				}
				if(u.usr.username.equals(player.game.getPlayer2())){
					player2 = u;
					Player2JoinedGame(player2);
				}
			}		
	}
	
	public void Player2JoinedGame(ServerThread player){
		player.SendMessage(player.game.getPlayer2(), "PLAYER2JOINED");
	}
	
	/** Methods from a tutorial */
    // Get an enumeration of all the OutputStreams, one for each client
    // connected to us  
    Enumeration<Object> getOutputStreams() {
    	return outputStreams.elements();
	}
   
    // Send a message to all clients (utility routine)
    void sendToAll( Object obj, String messageType ) {
    	// We synchronize on this because another thread might be
    	// calling removeConnection() and this would screw us up
    	// as we tried to walk through the list
    	synchronized( outputStreams ) {
    		Message msg = new Message();
    		msg.MessageType = messageType;
    		msg.MessageData = obj;
    		// For each client ...
    		for (Enumeration<Object> e = getOutputStreams(); e.hasMoreElements(); ) {
    			// ... get the output stream ...
    			ObjectOutputStream oout = (ObjectOutputStream)e.nextElement();
    			// ... and send the message
    			try {
    				oout.writeObject( msg );
    			} catch( IOException ie ) { System.out.println( ie ); }
    		}
    	}
    }
    
    // Remove a socket, and it's corresponding output stream, from our
    // list. This is usually called by a connection thread that has
    // discovered that the connection to the client is dead.
    void removeConnection( Socket s ) {
    	// Synchronize so we don't mess up sendToAll() while it walks
    	// down the list of all output streams
    	synchronized( outputStreams ) {
    		// Tell the world
    		System.out.println( "Removing connection to "+s );
    		// Remove it from our hashtable/list
    		outputStreams.remove( s );
    		// Make sure it's closed
    		try {
    			s.close();
    		} catch( IOException ie ) {
    			System.out.println( "Error closing "+s );
    			ie.printStackTrace();
    		}
    	}
    }
   
  
    String name = "";
    List<String> names = new ArrayList<String>();
    String chatMsg = "";
    
	private Hashtable<Object, Object> outputStreams = new Hashtable<Object, Object>();
	private Hashtable<String, GameObj> games = new Hashtable<String, GameObj>();
	private ArrayList<ServerThread> threads = new ArrayList<ServerThread>();

}
