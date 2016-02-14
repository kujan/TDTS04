//package proxyserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProxyServer {
    private static String[] badwords = {"spongebob", "britney spears", "paris hilton", "norrköping", "britneyspears", "parishilton"};

    public static void main(String[] args) throws IOException{

        try {


        // Get the command-line arguments: the host and port we are proxy for
        // and the local port that we listen for connections on
        //String host = "www.reddit.com";
        int remoteport = 80;
        int localport = Integer.parseInt(args[0]);
        runServer(remoteport, localport);
        } catch (Exception e) {
            System.err.println("Wrong startup arguments " + e);
        }
    }

    public static void runServer(int remoteport, int localport)
	throws IOException {
        ServerSocket ss = new ServerSocket(localport);

        
        
        //CharSequence getRequest = "";
        
        while(true) {
            
            final Socket client;
            client = ss.accept();
            System.out.println(client.isConnected());

            Thread t = new Thread() {
                public void run() {
                    byte[] request = new byte[1024];
                    //byte[] reply = new byte[4096];
                    int client_read = 0;
                    int server_read = 0;
                    String host = "";
                    CharSequence cs = "";
                    CharSequence getRequest = "";
                    Socket server = null;
                    InputStream from_client = null;
                    InputStream from_server = null;
                    OutputStream to_client = null;
                    OutputStream to_server = null;

                    //client -> proxy
                    try {

                        from_client = client.getInputStream();
                        to_client = client.getOutputStream();

                        System.out.println(from_client.available());
                        while((client_read = from_client.read(request)) != -1) {
                            
                            //save entire request from inputstream for later use
                            getRequest = new String(request, "US-ASCII");
                            Pattern p = Pattern.compile("Host:.(.*)");
                            Matcher m = p.matcher(getRequest);
                            
                            //find host in GET request
                            if (m.find()) {
                                host = m.group(1);
                                break;
                            }
                            
                            
                            }

                        System.out.println(host);
                    } catch (IOException e) {
                        System.err.println("error reading client request" + e);
                    }

                    //proxy -> server
                    try {
                    	//check GET request for bad URLs
                        if (checkRequest(getRequest)) {
                            server = new Socket(host, remoteport);
                            from_server = server.getInputStream();
                            to_server = server.getOutputStream();
                            //everything checks out, send bytes that we read
                            //earlier to server
                            to_server.write(request, 0, client_read);
                            to_server.flush();
                        }
                        else {
                                System.out.println("request contains bad URL");
                                PrintWriter out = new PrintWriter(new OutputStreamWriter(to_client));
                                //Bad URL detected, redirect to corresponding error page
                                out.println("HTTP/1.1 301 Moved Permanently\r\nLocation: http://www.ida.liu.se/~TDTS04/labs/2011/ass2/error1.html\r\n\r\n");
                                out.flush();   
                                //client.close();
                                //server.close();                                
                            }   
                        

                    } catch (IOException e) {
                        System.err.println("Error sending request to server" + e);
                    }
                    //server -> proxy -> client
                    try {
                    	//TODO: Fix logic to handle content checking properly, probably need to save multiple byte arrays of dynamic size depending on content-length in 
                    	//server response
                            int server_bytes = 0;
                            byte[] buffer = new byte[1024];
                            byte[] reply;
                            boolean header = false;
                            boolean badContent = false;

                            //System.out.println("From server: " + from_server.available());
                            while((server_bytes = from_server.read(buffer)) != -1) {
                                CharSequence serverCs = new String(buffer, "US-ASCII");

                                /*if (!header) {
                                    Pattern p = Pattern.compile("Content-Length: (.*)");
                                    Matcher m = p.matcher(serverCs);
                                    if (m.find()) {
                                            if (Integer.parseInt(m.group(1)) > 0) {
                                                reply = new byte[server_bytes + Integer.parseInt(m.group(1))];
                                                reply = buffer.clone();
                                                header = true;
                                            }
                                        }
                                    }*/
                                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                                outputStream.write( reply );
                                outputStream.write( buffer );

                                reply[] = outputStream.toByteArray(buffer.length + reply.length);

                                //reply = buffer.clone(h);

                                if (!checkResponse(serverCs)) {
                                    badContent = true;
                                    break;
                                    //to_client.write(reply, 0, server_bytes);
                                    //to_client.flush();
                                }
                                
                                    /*System.out.println("request contains bad content");
                                    PrintWriter out = new PrintWriter(new OutputStreamWriter(to_client));
                                    out.println("HTTP/1.1 301 Moved Permanently\r\nLocation: http://www.ida.liu.se/~TDTS04/labs/2011/ass2/error2.html\r\n\r\n");
                                    out.flush();   
                                    //client.close();
                                    //server.close(); 
                                    //break; */  
                                

                                //System.err.println(serverCs);
                                //System.err.println("in loop");
                                //System.err.println("stuck in here!");

                    }  
                    
                    if (!badContent) {
                        to_client.write(reply, 0, server_bytes);
                        to_client.flush();
                    }
                    else {
                        System.out.println("request contains bad content");
                        PrintWriter out = new PrintWriter(new OutputStreamWriter(to_client));
                        out.println("HTTP/1.1 301 Moved Permanently\r\nLocation: http://www.ida.liu.se/~TDTS04/labs/2011/ass2/error2.html\r\n\r\n");
                        out.flush(); 
                    }

                    } catch (IOException e) {
                        System.out.println("error sending reply to client" + e);
                    }
                    finally {
                            try {
                                if (client != null) {
                                    client.close();
                                }
                                if (server != null) {
                                    server.close();
                                }
                } catch (IOException e) {
                    System.err.println("Error closing sockets" + e);
                }
                    }
                }
            };

            t.start();
        }
    }
    private static boolean checkRequest(CharSequence cs) {
    Pattern p = Pattern.compile("(http:\\/\\/www\\S*)");
    Matcher m = p.matcher(cs);
    if (m.find()) {
        for (String s: badwords) {
            if (m.group(1).toLowerCase().contains(s.toLowerCase())) {
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
                return false;
            }
        }
        return true;
    }
}
            /*
            Socket client = null;
            Socket server = null;
            
            try {
                //accept connection
                client = ss.accept();
                final int bytes_read;
                CharSequence cs;
                String host = "";
                InputStream from_client = client.getInputStream();
                OutputStream to_client = client.getOutputStream();
                
                int read = 0;
                while((read = from_client.read(request)) != -1) {
                    
                    getRequest = new String(request, "US-ASCII");
                    Pattern p = Pattern.compile("Host:.(.*)");
                    Matcher m = p.matcher(getRequest);
                    
                    if (m.find()) {
                        host = m.group(1);
                        break;
                    }
                    
                   
                    cs = getRequest;
                }
                bytes_read = read;
                
                System.err.println(host);
                
                try {
                    server = new Socket(host, remoteport);
                } catch (IOException e) {
                    PrintWriter out = new PrintWriter(new OutputStreamWriter(to_client));
                    out.println("Can't connect to: " + host + " at port: " + remoteport);
                    out.flush();
                    client.close();
                }
                
                final InputStream from_server = server.getInputStream();
                final OutputStream to_server = server.getOutputStream();
                
                Thread c = new Thread() {
                    public void run() {
                        try {
                            
                            //write request from client to server
                            CharSequence a = new String(request, "US-ASCII");
                            System.err.println("request: " + a);
                            to_server.write(request, 0, bytes_read);
                            to_server.flush();
                        } catch (IOException e) {
                            System.err.println("Error sending client request to server" + e);
                        }
                        
                        try {
                            //to_server.close();
                            System.out.println("asdf");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                };
                
                c.start();
                
                
                try {
                    int server_bytes;
                    try {
                        //wait for server to have time to reply from thread
                        Thread.sleep(300);
                        System.out.println("Sleeping!");
                    } catch(InterruptedException ex) {
                            System.out.println("failed to sleep");
                            Thread.currentThread().interrupt();
                    }
                  
                    while((server_bytes = from_server.read(reply)) != -1) {
                        CharSequence serverCs = new String(reply, "US-ASCII");
                        System.err.println(serverCs);
                        //System.err.println("in loop");
                        System.err.println("stuck in here!");
                        to_client.write(reply, 0, server_bytes);
                        to_client.flush();
                    }  

                    try {
                        server.close();
                    } catch (IOException e) {
                        System.err.println("Error closing server socket " + e);
                    }
                } catch (IOException e) {
                    System.err.println("Error sending server reply to client " + e);
                }
                
                //done with replying to client
                System.err.println("closing client socket");
                to_client.close();
                
            } catch (Exception e) {
                System.err.println("Error closing to_client" + e);
                }
            
            finally {
                try {
                    if (server != null) {
                        server.close();
                    }
                    if (client != null) {
                        client.close();
                    }
                } catch (IOException e) {
                    System.err.println("Error closing sockets" + e);
                }
            }
        }
    }
}*/


    

