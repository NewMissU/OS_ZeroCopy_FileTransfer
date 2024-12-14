
// package OS_ZeroCopy_final;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.File;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.net.InetSocketAddress;

import java.nio.channels.FileChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;



public class Server {

    public static final String FILES_PATH = "D:\\code SU\\OS_Netbean\\files\\server\\";    
    public static final int PORT = 3030;
    public static final int PORT_CHANNEL = 3031;
    private static ArrayList<Socket> clientsList;
    
    public Server(){
        clientsList = new ArrayList<Socket>();
        try (ServerSocket serverSocket = new ServerSocket(PORT);
             ServerSocketChannel serverSocketChannel = ServerSocketChannel.open(); ){ //Server opened
            serverSocketChannel.socket().bind(new InetSocketAddress(PORT_CHANNEL));
            System.out.println("This is Server , start on port " + PORT );
            while(true){
                //Method acceptConnection
                Socket clientSocket = serverSocket.accept();
                SocketChannel clientSocketChannel = serverSocketChannel.accept();
                if(clientSocket.isConnected()){
                    clientsList.add(clientSocket); // add client to ArrayList
                    System.out.println("New client has connected to the server -> " + clientSocket + " #" +clientsList.size());
                    System.out.println("New clientSocketChannel has connected to the server -> " + clientSocketChannel);
//                    System.out.println("Amount of Client Connected : " + clientsList.size() );
                    new Thread(new ClientHandler(clientSocket,clientSocketChannel)).start();
                }
            }

        } catch (IOException e) {
            System.err.println("Error from Server()");
//            e.printStackTrace();
        }
    }
    
    private static class ClientHandler implements Runnable {

        private Socket clientSocket;
        private SocketChannel clientSocketChannel;
        private BufferedReader inTextFromClient;
        private BufferedOutputStream outBytetoClient;
        private PrintWriter outTextToClient;
        
        
        public ClientHandler(Socket socket, SocketChannel clientSocketChannel) {
            this.clientSocket = socket;
            this.clientSocketChannel = clientSocketChannel; //update
        }
        
        @Override
        public void run() {
            //(BufferedReader inTextFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream())); /*read data from client*/
            //PrintWriter outTextToClient = new PrintWriter(clientSocket.getOutputStream(), true); /*send data to client*/)
            try{
                inTextFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream())); /*read data from client*/
                outTextToClient = new PrintWriter(clientSocket.getOutputStream(), true); /*send data to client*/
                outBytetoClient = new BufferedOutputStream(clientSocket.getOutputStream());
                String clientCommand;
                while( (clientCommand = inTextFromClient.readLine()) != null ){
                    switch (clientCommand) {
                        case "/filelist":
                            //method filelist
                            fileList();
                            break;
                        case "/request":
//                            System.out.println("clientCommand: " + clientCommand);

                            //method download file
                            String fileName = inTextFromClient.readLine(); //recieve fileName
//                            System.out.println("fileName : " + fileName);
                            
                            //receieve message type of copy [Normal or ZeroCopy]
                            String type = inTextFromClient.readLine();
                            
                            //recieve file
                            File file = new File(FILES_PATH + fileName); //create file
                            if (file.exists()) {
                                outTextToClient.println("Long");
                                outTextToClient.println(file.length()); //length() -> return long
                                if(type.equals("1")){
                                    System.out.println("> " + clientSocket + " -> NormalCopy");
                                    long start = System.currentTimeMillis();
                                    //method sendNormal
                                    copy(file);
                                    long end = System.currentTimeMillis();
                                    long time = end - start;
                                    System.out.println("> Time " + time + " millisecond [NormalCopy] File: " + fileName);
                                }
                                else if(type.equals("2")){
                                    System.out.println("> " + clientSocket + " -> ZeroCopy");
                                    long start = System.currentTimeMillis();
                                    //method sendZeroCopy
                                    zeroCopy(file);
                                    long end = System.currentTimeMillis();
                                    long time = end - start;
                                    System.out.println("> Time " + time + " millisecond [ZeroCopy] File: " + fileName);
                                }
                            }
                            else{
                                outTextToClient.println("String");
                                outTextToClient.println("File Not Found, Please try agian");
                            }
                            break;
                        case "/exit":
                            //shutdown client
                            inTextFromClient.close();
                            outTextToClient.close();
                            clientSocketChannel.close();
                            if(clientSocket.isConnected()){
                                System.out.println("Server Said :");
                                System.err.println("Client (" + clientSocket + ") has disconnected");
                                clientsList.remove(clientSocket);
                                System.err.println("Amount of Client Connected Left : " + clientsList.size());
                                clientSocket.close();
                            }
                            break;
                        default:
                            System.out.println("Wrong command , Please try again");
                            break;
                    }
                }
            }
            catch (IOException e) {
                try {
                    if(clientSocket.isConnected()){
//                    System.err.println("Client #"+ clientsList.size() +"(" + clientSocket + ") has disconnected");
                    clientsList.remove(clientSocket);
                    clientSocket.close();
//                    System.err.println("Amount of Client Connected Left : " + clientsList.size());
                    }
                } catch (IOException ex) {
                    e.printStackTrace();
                }
//                e.printStackTrace();
            }
//            finally{
//                System.out.println("Final Amount of Client Connected : " + clientsList.size() );
//            }

        }
    
        public void fileList(){
            String allFilesInServer = "** Files **\n";
            File[] fileList = new File(FILES_PATH).listFiles();
            for(int i=0 ; i<fileList.length ; i++){
                //concat string
                allFilesInServer += String.format("* [%d] - %s\n",i+1,fileList[i].getName()); 
            }
            allFilesInServer += "** End Files **";
            outTextToClient.println(allFilesInServer);
            // send End signal because Bufferedreader.readline() need EOF if we don't have EOF readline() will not return null
            outTextToClient.println("EOF");
            
            System.out.println("Send fileList to Client successful");
        }
    
        public void copy(File file){
            System.out.println("File Copying");
    //        OutputStream outStreamtoClient = null;
            //BufferedOutputStream outBytetoClient = new BufferedOutputStream(clientSocket.getOutputStream());
            try(BufferedInputStream inByteFromFile = new BufferedInputStream(new FileInputStream(file))){ //read file to byte
                if(file.exists()){
                    // send End signal because InputStream.read(buffer) need EOF to know when need to end
//                    outTextToClient.println(file.length()); //length() -> return long

                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    long totalBytesRead = 0;
                    while( totalBytesRead < file.length() && ( bytesRead = inByteFromFile.read(buffer) ) != -1 ){
                        if (totalBytesRead + bytesRead > file.length()) {
                            bytesRead = (int)(file.length() - totalBytesRead);
                        }
                        totalBytesRead += bytesRead;
                        System.out.println("Bytes read: " + bytesRead + ", Total bytes read: " + totalBytesRead);
                        outBytetoClient.write(buffer, 0, bytesRead);
                    }
                    
                    outBytetoClient.flush(); // Ensure all data is sent
//                    outStreamtoClient.close();  
                    if (totalBytesRead != file.length()) {
                        System.err.println("Warning: File size mismatch. Expected: " + file.length() + ", but got: " + totalBytesRead);
                    } else {
                        System.out.println("Filesize : " + file.length());
                        System.out.println("checkSentSize : " + totalBytesRead);
                        System.out.println("Send File download to client complete!");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } 
        }
        
        public void zeroCopy(File file){
            try(FileChannel sourceFile = new FileInputStream(file).getChannel() ){
//                sourceFile.transferTo(0, sourceFile.size(), destinationClient);
                long position = 0;
                long count;
                //sourceFile.size() - position
                while (position < file.length() && (count = sourceFile.transferTo(position, sourceFile.size() - position, clientSocketChannel)) > 0) {
                    position += count;
//                    System.out.println("CountRead: " + count);
                    System.out.println("Transferred: " + count + " bytes, Total transferred: " + position + " bytes");
                }
                
//                long bytesTransferred = sourceFile.transferTo(0, sourceFile.size(), clientSocketChannel);
        
                if (position == file.length()) {
                    System.out.println("File download complete!");
                } else {
                    System.err.println("Warning: Not all bytes transferred. Expected: " + file.length() + ", Transferred: " + position);
                }
            }
            catch(IOException e){
                e.printStackTrace();
            }
        }
        
    }
    
    public static void main(String[] args) {
        new Server();
    }
}

