import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

public class Server {
	/** Data Members */
    String userListNames = ""; // Holds names for userlist
    List<String> namesLoggedIn = new ArrayList<String>(); // Holds names to test against for duplicate logins
    String chatMsg = "";
    
	private Hashtable<Object, Object> outputStreams = new Hashtable<Object, Object>();
	private Hashtable<String, GameObj> games = new Hashtable<String, GameObj>();
	private ArrayList<ServerThread> threads = new ArrayList<ServerThread>();
	
	/** Main */
	public static void main(String[] args){
		System.out.println("Server Starting");
		
		// Create the Server
		Server foo = new Server(40000);
	}
	
	/** Server Constructor */
	public Server(int port){
		System.out.println("Initializing server.");
				
		try{
			ServerSocket serverSocket = new ServerSocket(port);
			
			// Tell the world we're ready to go 
			// -http://www.cn-java.com/download/data/book/socket_chat.pdf
			System.out.println( "Listening on "+ serverSocket );			
			
			while(true){
				Socket communicationSocket = serverSocket.accept();
				
				// Tell the world we've got it
				// -http://www.cn-java.com/download/data/book/socket_chat.pdf
				System.out.println( "Connection from "+ communicationSocket );
				
				// Create a DataOutputStream for writing data to the
				// other side  -http://www.cn-java.com/download/data/book/socket_chat.pdf
				ObjectOutputStream oout = new ObjectOutputStream( communicationSocket.getOutputStream() );
				
				// Save this stream so we don't need to make it again
				// -http://www.cn-java.com/download/data/book/socket_chat.pdf
				outputStreams.put( communicationSocket, oout );
				
				// Create a new thread for this connection, and then forget
				// about it -  -http://www.cn-java.com/download/data/book/socket_chat.pdf
				// Add to arraylist threads to use later
				threads.add( new ServerThread( this, communicationSocket, oout ) );

			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	/***********************************************************
	 *			 		Login/Logout Methods		 		   *
	 ***********************************************************/
	public void isDuplicate(String username, ServerThread curThread) {
		// If username is already logged in, 
		// send duplicate login message, else the login passed.
		if(namesLoggedIn.contains(username)){
			curThread.SendMessage(new String(), "DUPLICATELOGIN");			
		}
		else{
			curThread.SendMessage(username, "PASSED");
		}
    }
	
	public void loggedIn(String username) {
		// User is logged in, add usernamed to list of names logged in
		namesLoggedIn.add(username);
	}
	
	public void loggedOut(String username) {
		// User has logged out, remove username from names logged in
		namesLoggedIn.remove(username);
	}
	
	/***********************************************************
	 *			Updating User List in Game Methods			   *
	 ***********************************************************/
	public void sendlisttoyou(String username) {
		// Add username to userListNames, and send the updated list
		// As well as a chat message that user has entered
		userListNames += username + "\n";
	    sendToAll(userListNames, "USERNAMES");
	    sendToAll(username, "ENTERED");
    }
	
	public void updateUserList(String username) {
		// If the userList names contains the user name in the list
		// Replace the name with and line break with an empty string
		// Then send a chat message that the user has exited
		if(userListNames.contains(username)){
			userListNames = userListNames.replace(username + "\n", "");
	        sendToAll(userListNames, "USERNAMES");
	        sendToAll(username, "EXITED");
		}		
    }
    
	/***********************************************************
	 *				Sending Chat Messages Method			   *
	 ***********************************************************/
	public void sendChats(String username, String msg){
		// Accept message and username, add message
		// to chatMsg and add username to front of message
		chatMsg = msg + "\n";
		sendToAll(new String(), username + " " + chatMsg);
	}
		
	/**********************************************************
	 *		 Game Methods - Starting and Joining a Game		  *
	 **********************************************************/
	public void setUpNewGame(String username, ServerThread curThread){
		// User is setting up a new game, 
		// Add new game with user name as key, (user is first player)
		games.put(username, new GameObj(username));
		
		// Assign the new game to curThread game
		curThread.game = games.get(username);
		System.out.println(games.get(username) + " was created.");
	}
	
	public void findGameToJoin(String username, ServerThread curThread){
		boolean joinedGameSuccess = false; // Keep track of whether finding a game is a success
	    
		// Enumerate through Games, check if game has a player 2
		for (Enumeration<GameObj> e = games.elements(); e.hasMoreElements(); ) {
			GameObj game = (GameObj)e.nextElement();
			
			if(game.getPlayer2() == ""){
				System.out.println("Player2 for this game is in fact null.");
				// Assign the game found with empty player2 slot to curThreads game
				curThread.game = game;
				
				// Set the games player 2
				curThread.game.setPlayer2(username);
				
				// The game is now not open!
				curThread.game.isOpenGame = false;	
				
				// Player 2 is going to join a game, send the updated list of users for
				// userPanel
				sendlisttoyou(username);
				
				// User has Join a game! Change joinedGame to a success
				System.out.println("The user " + username + " has joined the game: " + game);
				joinedGameSuccess = true;
				
				// Now update the joined Game for all threads with that game
				// Then break out of loop
				updateJoinedGame(curThread);	
				break;
			}
		}
	    
		// No empty games were found, so send message to client that join has failed
		if(!joinedGameSuccess){
			curThread.SendMessage(new String(), "JOINFAILED");
			System.out.println("Joining a game failed.");
		}

	}
	
	public void updateJoinedGame(ServerThread player){
		// Find games, and assign turns for user, send that game through
		for(ServerThread u: threads){
			if(u.usr.username.equals(player.game.getPlayer1())){
				u.game.isTurn = true;
				Player2JoinedGame(u);
			}
			if(u.usr.username.equals(player.game.getPlayer2())){
				u.game.isTurn = false;
				Player2JoinedGame(u);
			}
		}	
	}
	
	public void Player2JoinedGame(ServerThread player){
		// Send Message that player2 has joined with game
		player.SendMessage(player.game, "PLAYER2JOINED");
	}
	
	/**********************************************************
	 *		  Game Methods - Making Moves in The Game		  *
	 **********************************************************/
	public void updatePlayerMove(ServerThread player, int row, int col){
		int playerNumber = 0; 	// Keep track of player that made move
		boolean isWon  = false; // Has a player won yet?
		boolean isFull = false; // Is the game filled up yet?
		
		if(player.usr.username.equals(player.game.getPlayer1())){
			// Set the cell for player 1, check if player 1 has won
			player.game.setCell(row, col, 'X');
			isWon = player.game.isWon('X');
			playerNumber = 1;
		}
		else{
			// Set the cell for player 2, check if player 2 has won
			player.game.setCell(row, col, 'O');
			isWon = player.game.isWon('O');
			playerNumber = 2;			
		}	
		
		// If there is no winners, check to see if game is full yet
		if(!isWon){
			isFull = player.game.isFull();
		}
		
		// Update Game
		sendUpdatedGame(player, playerNumber, isWon, isFull);
	}
	
	public void sendUpdatedGame(ServerThread player, int playerNum, boolean isWon, boolean isFull){
		for(ServerThread u: threads){
			if(isWon){
				if(u.game.player1.equals(player.game.getPlayer1())){
					u.game.winner = player.usr.username;
					u.game.isTurn = false;
					u.SendMessage(u.game, "GAMEOVER");
				}
			}
			else if(isFull){
				if(u.game.player1.equals(player.game.getPlayer1())){
					u.game.winner = "Draw";
					u.game.isTurn = false;
					u.SendMessage(u.game, "GAMEOVER");
				}
			}
			else{
				// Not sure how to write shorter without updating game to have same exact
				// turn
				if(u.usr.username.equals(player.game.getPlayer1()) && playerNum == 1){
					u.game.isTurn = false;
					u.SendMessage(u.game, "UPDATEDGAME");
				}
				if(u.usr.username.equals(player.game.getPlayer1()) && playerNum == 2){
					u.game.isTurn = true;
					u.SendMessage(u.game, "UPDATEDGAME");
				}
				if(u.usr.username.equals(player.game.getPlayer2()) && playerNum == 1){
					u.game.isTurn = true;
					u.SendMessage(u.game, "UPDATEDGAME");
				}
				if(u.usr.username.equals(player.game.getPlayer2()) && playerNum == 2){
					u.game.isTurn = false;
					u.SendMessage(u.game, "UPDATEDGAME");
				}
			}
		}
	}
	
	/**********************************************************
	 *			  Game Methods - Quitting a Game			  *
	 **********************************************************/
	public void quitGame(ServerThread player){		
		// Get Game
		GameObj game = games.get(player.game.player1);
		
		// Save game key - To remove game later
		String gameKey = game.player1;
		
		System.out.println("We have recieved game key " + games.get(player.game.player1));
		
		// Assign empty string to player from whichever user is quitting
		if(game.player1.equals(player.usr.username)){
			game.player1 = "";
		}
		if(game.player2.equals(player.usr.username)){
			game.player2 = "";
		}
		
		// If both players are empty, the game is empty, just remove the game
		if(game.player2.equals("") && game.player1.equals("")){
			// All players have quit, remove game
			System.out.println("We have removed game with key " + gameKey);
			games.remove(gameKey);
		}
		else{
			// Update for leftover player
			for(ServerThread u: threads){
				// If thread contains game, and thread is not the same thread that just quit game
				// Then update the game for thread
				if(u.game.equals(player.game) && !u.equals(player)){
					// Be lazy, just remove key, make leftover player number 1
					// And assign a new game to left over player thread,
					// Send new game to client and message that player left game
					games.remove(gameKey);
					games.put(u.usr.username, new GameObj(u.usr.username));
					u.game = games.get(u.usr.username);
					u.SendMessage(u.game, "PLAYERLEFTGAME");
				}
			}
		}		
	}
	
	/****************************************************************
	 *				Send To All - Using OutputStreams				*
	 *						From Tutorial 							*
	 *	-http://www.cn-java.com/download/data/book/socket_chat.pdf 	*
	 ****************************************************************/
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
    void removeConnection( Socket s, ServerThread curThread ) {
    	// Synchronize so we don't mess up sendToAll() while it walks
    	// down the list of all output streams
    	synchronized( outputStreams ) {
    		// Tell the world
    		System.out.println( "Removing connection to "+s );
    		// Remove it from our hashtable/list
    		outputStreams.remove( s );
    		
    		// Remove current thread as well when removing connection,
    		// so that we don't reiterate through a thread that no longer
    		// exists
    		threads.remove( curThread );
    		// Make sure it's closed
    		try {
    			s.close();
    		} catch( IOException ie ) {
    			System.out.println( "Error closing "+s );
    			ie.printStackTrace();
    		}
    	}
    }


}
