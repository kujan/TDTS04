import java.net.*;
import java.util.ArrayList;
import java.util.Scanner;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProxyServer {
	/** The main method parses arguments and passes them to runServer */
	private static boolean badWord = false;
	private static boolean badURL = false;
	
	final static String error1 = "Sorry, but the Web page that you were trying to access is inappropriate for you, based on some of the words it contains. The page has been blocked to avoid insulting your intelligence. \nNet Ninny";
	private static String error2 = "";
	private static String[] badwords = {"spongebob", "britney spears", "paris hilton", "norrköping", "britneyspears", "parishilton"};
	public static void main(String[] args) throws IOException {
		try {
			
			
			// Get the command-line arguments: the host and port we are proxy for
			// and the local port that we listen for connections on
			//String host = "www.reddit.com";
			int remoteport = 80;
			int localport = 6066;
			
			// Print a start-up message
			System.out.println("Starting proxy for "  + ":" + remoteport +
				" on port " + localport);
			
			// And start running the server
			runServer(remoteport, localport);   // never returns
		}
		catch (Exception e) {
			System.err.println(e);
			System.err.println("Usage: java SimpleProxyServer " +
			"<host> <remoteport> <localport>");
		}
	}
	
	/**
	* This method runs a single-threaded proxy server for
	* host:remoteport on the specified local port.  It never returns.
	**/
	public static void runServer(int remoteport, int localport)
	throws IOException {
		// Create a ServerSocket to listen for connections with
		ServerSocket ss = new ServerSocket(localport);
		
		// Create buffers for client-to-server and server-to-client communication.
		// We make one final so it can be used in an anonymous class below.
		// Note the assumptions about the volume of traffic in each direction...
		final byte[] request = new byte[1024];
		byte[] reply = new byte[4096];
		boolean triggered = false;
		CharSequence getRequest = "";
		
		// This is a server that never returns, so enter an infinite loop.
		while(true) {
			// Variables to hold the sockets to the client and to the server.
			Socket client = null, server = null;
			try {
				// Wait for a connection on the local port
				client = ss.accept();
				final int bytes_read;
                                final CharSequence cs;
				// Get client streams.  Make them final so they can
				// be used in the anonymous thread below.
				InputStream from_client = client.getInputStream();
				final OutputStream to_client= client.getOutputStream();
				
				String host = "";
				
				int read = 0;
				
				while((read = from_client.read(request)) != -1) {
                                 
					System.out.println("a");
					getRequest =  new String(request, "US-ASCII");
					Pattern p = Pattern.compile("Host:.(.*)");
					Matcher m = p.matcher(getRequest);
					
					if (m.find()) {
						System.out.println("ADFASFSAFSAFDSAFSAFSA");
						host = m.group(1);
						break;
					}
                                }
                                System.out.println("af");
                                bytes_read = read;
                                cs = getRequest;
					// Make a connection to the real server
					// If we cannot connect to the server, send an error to the
					// client, disconnect, then continue waiting for another connection.
					try { server = new Socket(host, remoteport); }
					catch (IOException e) {
						PrintWriter out = new PrintWriter(new OutputStreamWriter(to_client));
						out.println("Proxy server cannot connect to " + host + ":" +
						remoteport + ":\n" + e);
						out.flush();
						client.close();
						continue;
					}
					
					// Get server streams.
					final InputStream from_server = server.getInputStream();
					final OutputStream to_server = server.getOutputStream();
					
					// Make a thread to read the client's requests and pass them to the
					// server.  We have to use a separate thread because requests and
					// responses may be asynchronous.
					Thread t = new Thread() {
						public void run() {
							//int bytes_read;
							try {
								System.out.println("in thread");
								//CharSequence cs = new String(request, "US-ASCII");
								//System.out.println("======CLIENT REQUEST =========");
								//System.out.println(cs);
								System.out.println("checking request");
								if (checkRequest(cs)) {
									System.out.println("request is OK");
                                                                        System.out.println(bytes_read);
                                                                        
									to_server.write(request, 0, bytes_read);
									to_server.flush();
								}
								else {
									System.out.println("request is not OK");
									if (badURL) {
										System.out.println("request contains bad URL");
										PrintWriter out = new PrintWriter(new OutputStreamWriter(to_client));
										out.println("HTTP/1.1 302 Found\r\nLocation: error1.html\r\n\r\n");
										out.flush();
										//to_server.close();
										//client.close();
										
									}
								}
								
								
							}
							catch (IOException e) {System.out.println(e);}
							
							// the client closed the connection to us, so  close our
							// connection to the server.  This will also cause the
							// server-to-client loop in the main thread exit.
							try {to_server.close();} catch (IOException e) {}
						}
					};
					
					// Start the client-to-server request thread running
					t.start();
					
					// Meanwhile, in the main thread, read the server's responses
					// and pass them back to the client.  This will be done in
					// parallel with the client-to-server request thread above.
					//int bytes_read;
					try {
						try {
							Thread.sleep(100);
						} catch(InterruptedException ex) {
							System.out.println("failed to sleep");
							Thread.currentThread().interrupt();
						}
						int server_bytes;
                                                if (server.isConnected()) {
                                                    System.out.println("Server socket is open");
                                                }
                                                if (client.isConnected()) {
                                                    System.out.println("Client socket is open");
                                                }
						System.out.println("entering loop for server response");
                                                //System.out.println(from_server.available());
						while((server_bytes = from_server.read(reply)) != -1) {
							System.out.println("in loop");
							CharSequence serverCs = new String(reply, "US-ASCII");
							System.out.println("======SERVER REPLY=======");
							//System.out.println(x);
							System.out.println(serverCs);
							System.out.println("checking server response");
							if (checkResponse(serverCs)) {
								System.out.println("server response is OK");
								to_client.write(reply, 0, server_bytes);
								to_client.flush();
							}
							
							else {
								System.out.println("server response is not OK");
								if (badWord) {
									System.out.println("server response contains bad words");
									System.out.println("BAD WORD MOTHERFucKER!");
									PrintWriter out = new PrintWriter(new OutputStreamWriter(to_client));
									out.println("HTTP/1.1 302 Found\r\nLocation: error2.html\r\n\r\n");
									out.flush();
									//to_server.close();
									//to_client.close();
									//client.close();
									//server.close();
								}
								
							}
						}
						
						
					}
					catch(IOException e) {System.out.println(e);}
					
					System.out.println("Something went wrong, client socket closed");
					// The server closed its connection to us, so close our
					// connection to our client.  This will make the other thread exit.
					to_client.close();
				
				
				// Close the sockets no matter what happens each time through the loop.
					
			}
                        catch (IOException e) { System.err.println(e); }
                        
                        finally {
					try {
						if (server != null) server.close();
						if (client != null) client.close();
					}
					catch(IOException e) {}
				}	
		}
        }
	private static boolean checkRequest(CharSequence cs) {
		Pattern p = Pattern.compile("(http:\\/\\/www\\S*)");
		Matcher m = p.matcher(cs);
		if (m.find()) {
			for (String s: badwords) {
				if (m.group(1).toLowerCase().contains(s.toLowerCase())) {
					badURL = true;
					return false;
				}
			}
			return true;
		}
		
		return true;
	}
	private static boolean checkResponse(CharSequence cs) {
		//return true;
		for (String s: badwords) {
			if (cs.toString().toLowerCase().contains(s.toLowerCase())) {
				badWord = true;
				return false;
			}
		}
		return true;
	}
}