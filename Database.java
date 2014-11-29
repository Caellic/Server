import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;


public class Database {
	private static java.sql.PreparedStatement preparedStatement = null;
	private static java.sql.PreparedStatement preparedStatement1 = null;
	private static java.sql.PreparedStatement preparedStatement2 = null;
	private static ResultSet rset = null;
	private boolean valid = false;
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
			
			// Create a statement
			String query = "select UserName, UserPassword from users" + 
	    			" where UserName= ? and UserPassword= ?";


			String query1 = "select UserName from users where UserName= ?";
			
			String query2 = "insert into users (UserName, UserPassword)" + 
	    			" values (?, ?)";
						
			// Create a statement
			preparedStatement = connection.prepareStatement(query);
			preparedStatement1 = connection.prepareStatement(query1);
			preparedStatement2 = connection.prepareStatement(query2);
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	 public void executeLoginQuery(String userName1, String password1){
		 valid = false;
	        try{
				preparedStatement.setString(1, userName1);
				preparedStatement.setString(2, password1);
				rset = preparedStatement.executeQuery();								
				
				if (rset.next()){
					if(userName1.equals(rset.getString("UserName")) && 
						password1.equals(rset.getString("UserPassword"))){
							valid = true;
					}
				}
	        }
	        catch (Exception e) {
	            e.printStackTrace();
	        }
	}
	 
	 public void executeValidateRegistrationQuery(String userName1){
		 valid = true;
	        try{
				preparedStatement1.setString(1, userName1);
				rset = preparedStatement1.executeQuery();								
				
				if (rset.next()){
					if(userName1.equals(rset.getString("UserName"))){
							valid = false;
					}
				}
	        }
	        catch (Exception e) {
	            e.printStackTrace();
	        }
	}
	 
	 public void executeRegistrationQuery(String userName1, String password1){
		// valid = false;
	        try{
				preparedStatement2.setString(1, userName1);
				preparedStatement2.setString(2, password1);
				preparedStatement2.executeUpdate();								
				
	        }
	        catch (Exception e) {
	            e.printStackTrace();
	        }
	}
	 
	 public boolean getValid(){
		 return valid;
	 }
}
