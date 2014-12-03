import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;

public class Database {
	private static java.sql.PreparedStatement preparedStatement = null;
	private static java.sql.PreparedStatement preparedStatement1 = null;
	private static java.sql.PreparedStatement preparedStatement2 = null;
	private static ResultSet rset = null;
	
    //Connection
    public Connection connection = null;
	
	public void InitializeDB(){
	
		try {
			// Load the JDBC driver
			Class.forName("com.mysql.jdbc.Driver");
			System.out.println("Driver loaded");
			
			// Connect to a database
			Connection connection = DriverManager.getConnection
				("jdbc:mysql://68.12.24.23:3306/TicTacToeUsers" , "Allira", "java");
			System.out.println("Database connected");
			
			// Create a statements
			String querySelectUserPass = "select UserName, UserPassword from users" + 
	    			" where UserName= ? and UserPassword= ?";

			String querySelectUser = "select UserName from users where UserName= ?";
			
			String queryInsertUserPass = "insert into users (UserName, UserPassword)" + 
	    			" values (?, ?)";
						
			// Prepare statements
			preparedStatement  = connection.prepareStatement(querySelectUserPass);
			preparedStatement1 = connection.prepareStatement(querySelectUser);
			preparedStatement2 = connection.prepareStatement(queryInsertUserPass);
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/** Check if user and pass exists in database */
	public boolean executeLoginQuery(String userName1, String password1){
		 boolean result = false;
		 
		 try{
			 preparedStatement.setString(1, userName1);
			 preparedStatement.setString(2, password1);
			 rset = preparedStatement.executeQuery();								
				
			 if (rset.next()){
				 if(userName1.equals(rset.getString("UserName")) && 
						 password1.equals(rset.getString("UserPassword"))){
					 result = true;
				 }
			 }
		 }
		 catch (Exception e) {
			 e.printStackTrace();
		 }
	        
		 return result;
	}
	
	/** Check if user already exists in database */
	public boolean executeValidateRegistrationQuery(String userName1){		 
		 boolean result = true;
		 
		 try{
			 preparedStatement1.setString(1, userName1);
			 rset = preparedStatement1.executeQuery();								
				
			 if (rset.next()){
				 // If user name is equal to the rset, valid = false
				 if(userName1.toLowerCase().equals(rset.getString("UserName").toLowerCase())){
					 System.out.println("Never hits");
					 result = false;
				 }
			 }				
		 }
		 catch (Exception e) {
			 e.printStackTrace();
		 }
	        
		 return result;
	}
	
	/** Insert user and pass into database */
	public void executeRegistrationQuery(String userName1, String password1){
		 try{
			preparedStatement2.setString(1, userName1);
			preparedStatement2.setString(2, password1);
			preparedStatement2.executeUpdate();
		}
	    catch (Exception e) {
	    	e.printStackTrace();
	    }
	}
}
