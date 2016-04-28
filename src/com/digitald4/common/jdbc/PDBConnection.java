/*
 * Copyright (c) 2002-2010 ESP Suite. All Rights Reserved.
 *
 *     
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE 
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 * 
 * Authors: Technology Integration Group, SCE
 * Developers: Eddie Mayfield, Frank Gonzales, Augustin Muniz,
 * Kate Suwan, Hiro Kushida, Andrew McNaughton, Brian Stonerock,
 * Russell Ragsdale, Patrick Ridge, Everett Aragon.
 * 
 */
package com.digitald4.common.jdbc;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * PDBConnection class creates a singleton object that contains
 * a persistent data base connection.  pdbc is declared static
 * to allow only one copy of the object in memory, thus only one
 * connection is used through the duration of the session.
 *
 * @author Distribution Staff Engineering
 * @version 2.0
 */
public class PDBConnection{
	public final static String driver = "oracle.jdbc.OracleDriver";
	public static final String SCHEMA="MDI";
	private static PDBConnection pdbc;
	private Connection con;
	private boolean notLoaded=true;
	public String url = null;
	private String user;
	private String pass;
	private boolean enabled=true;
	private String clientId;
	private int failCount=0;
	
	static{
		try {
			Class.forName("oracle.jdbc.OracleDriver");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * A protected default constructor.  Its access specifier is set
	 * as protected which disallows instantiation of the object.
	 * @throws IOException 
	 */
	protected PDBConnection(String url, String user, String pass) throws IOException{
		this.url=url;
		this.user=user;
		this.pass=pass;
	}

	/**
	 * Static method that checks to see if the static pdbc object
	 * is null, if so then a new PDBConnection object is made and passed.
	 * If pdbc is not null then it is passed.
	 *
	 * @return the singleton pdbc object.
	 */
	public static PDBConnection getInstance(){
		return pdbc;
	}

	/**
	 * Static method that creates a new PDBConnection object if pdbc is
	 * null and sets the object's fields with the given parameters.
	 * @param url url
	 * @param user user name
	 * @param pass password
	 * @return the singleton pdbc object.
	 * @throws IOException 
	 */
	public static PDBConnection getHiddentInstance(String url, String user, String pass) throws IOException{
		if(pdbc == null || pdbc.getURL() != url || pdbc.getUser() != user){
			pdbc = new PDBConnection(url, user, pass);
		}
		return pdbc;
	}

	public void setEnabled(boolean enabled){
		this.enabled = enabled;
	}

	public boolean isEnabled() {
		return enabled;
	}
	
	public static Connection getTemporaryConnection(String url, String user, String pass) throws SQLException{
		Connection tempConn=null;
		
		tempConn = DriverManager.getConnection(url,user, pass); 
		return tempConn;
	}
	
    public Connection getConnection() throws SQLException{
    	if(!enabled)
    		return null;
    	if(notLoaded) {
	    	try {
				Class.forName(driver);
				notLoaded=false;
			} catch (Exception e) {
				throw new SQLException("Database Driver Error: "+e.getMessage());
			}
		}
		if(con == null || con.isClosed()){
			if(failCount>3) throw new SQLException("Exceed attempts without a successful connection");
			failCount++; //Increment fail count 
			con = new StatsConnection(DriverManager.getConnection(url,user, pass)); 
			//           ((OracleConnection)con).setDefaultRowPrefetch (512);//default is 10!!!
			failCount=0; //Clear fail count on correct login
			createSession();
			createAppModule();
		}
		return con;
	}

	/**
	 * Updates session for using MDI application objects in the database.
	 * @throws SQLException
	 */
	private void createSession() throws SQLException{
		String sqlQuery = "alter session set current_schema = MDI";
		PreparedStatement psQuery = getConnection().prepareStatement(sqlQuery);
		psQuery.executeQuery();
		psQuery.close();
	}

	/**
	 * Specifies the application module.
	 * @throws SQLException
	 */
	protected void createAppModule() throws SQLException{
		//String[] metrics = new String[OracleConnection.END_TO_END_STATE_INDEX_MAX];
		//metrics[OracleConnection.END_TO_END_MODULE_INDEX] = "MDI";
//		metrics[OracleConnection.END_TO_END_ACTION_INDEX] = "Action";
		//((OracleConnection)con).setEndToEndMetrics(metrics, (short)0);
	}

	/**
	 * Closes the connection.
	 * @throws SQLException
	 */
	public void closeConnection() throws SQLException{
		if(con != null && !con.isClosed())
			con.close();
	}

	/**
	 *
	 * @param url
	 */
	public void setURL(String url){
		this.url = url;
	}

	/**
	 *
	 * @return
	 */
	public String getURL(){
		return url;
	}

	/**
	 *
	 * @param user
	 */
	public void setUser(String user){
		this.user = user;
	}

	/**
	 *
	 * @return
	 */
	public String getUser(){
		return user;
	}

	/**
	 *
	 * @param pass
	 */
	public void setPass(String pass){
		this.pass = pass;
	}

	/**
	 *
	 * @return
	 * @throws SQLException
	 */
	public int getCurrentOpenCursors()throws SQLException{
		return getCurrentOpenCursors(getConnection());
	}
	/**
	 * Returns the number of open cursors on an Oracle Database.
	 * @return  Number of currently open cursors - note that this method will produce one of these
	 *          but it cleanly closes it so will not accumulate them.
	 * @param enhanceConnection Connection to the Oracle Database
	 */
	public int getCurrentOpenCursors(Connection conn) {
		PreparedStatement psQuery = null;
		ResultSet rs = null;

		int cursors = -1;
		try {
			String sqlQuery = "select count(*) AS COUNT from v$open_cursor where user_name like '"+user+"'";
			psQuery = conn.prepareStatement(sqlQuery);
			rs = psQuery.executeQuery();

			if (rs.next()) {
				cursors = rs.getInt("COUNT");
			}
		} catch (SQLException e) {
			System.out.println("SQLException in getCurrentOpenCursors(Connection conn): "+e);
		} finally {
			try {
				if (rs != null) {rs.close();}
				if (psQuery != null) {psQuery.close();}
			}
			catch (SQLException ex)  {
				System.out.println("A SQLException error has occured in getCurrentOpenCursors(Connection conn): " + ex.getMessage());
				ex.printStackTrace();
			}
		}
		return cursors;
	}

	/**
	 * Enables the system proxy for internet access.
	 */
	public static void enableProxy(){
		System.getProperties().put( "proxyHost", "proxy.sce.com" );
		System.getProperties().put( "proxyPort", "80" );
		System.getProperties().put( "proxySet", "true" );
	}

	/**
	 * Disables system proxy
	 */
	public static void disableProxy(){
		System.getProperties().put( "proxySet", "false" );
	}

	public String getTableDescriptions(){
		return "SELECT t.table_name, t.comments FROM all_tab_comments t WHERE t.owner = 'MDI' AND t.comments IS NOT NULL ORDER BY t.table_name";
	}

	public String getClientId() throws SQLException{
		if(clientId == null){
			PreparedStatement ps = getConnection().prepareStatement("SELECT SYS_CONTEXT('USERENV','CLIENT_IDENTIFIER') FROM DUAL");
			ResultSet rs = ps.executeQuery();
			if(rs.next())
				clientId = rs.getString(1);
			rs.close();
			ps.close();
		}
		return clientId;
	}
	public void disableLogger() throws SQLException{
		CallableStatement cs = getConnection().prepareCall("{call DBMS_SESSION.SET_IDENTIFIER(?)}");
		cs.setString(1,"NON LOGGER");
		cs.execute();
		cs.close();
	}
	public void enableLogger() throws SQLException{
		Connection con = PDBConnection.getInstance().getConnection();
		CallableStatement cs = con.prepareCall("{call DBMS_SESSION.SET_IDENTIFIER(?)}");
		cs.setString(1, getClientId());
		cs.execute();
		cs.close();
	}

	public void disable() {
		enabled=false;
	}
	public void enable() {
		enabled=true;
	}
}