
// package OS_ZeroCopy_final;


import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

import java.util.Scanner;
import java.nio.channels.FileChannel;

import java.nio.channels.SocketChannel;




public class Client {
    
//    public static final String IPSERVER = "192.168.17.114"; //10.0.2.15 , 127.0.0.1 , 192.168.1.33
    public static final String IPSERVER = "127.0.0.1"; //10.0.2.15 , 127.0.0.1 , 192.168.1.33
    public static final int PORT = 3030;
    public static final int PORT_CHANNEL = 3031;
    private static final String CLIENT_FILEPATH = "D:\\code SU\\OS_Netbean\\files\\client\\";
    private Scanner scanner;
    private BufferedReader inTextFromServer;
    private PrintWriter outTextToServer;
    private BufferedInputStream inByteFromServer;
    private Socket clientSocket;
    private SocketChannel clientSocketChannel;
    
    public Client(){
        try{
            clientSocket = new Socket(IPSERVER , PORT);
            clientSocketChannel = SocketChannel.open(new InetSocketAddress(IPSERVER, PORT_CHANNEL));
            System.out.println("This is Client Site");
            System.out.println("Client has connected to server -> (" + clientSocket + ")");
            System.out.println("ClientSocketChannel has connected to server -> (" + clientSocketChannel + ")");
            //Create object
            this.scanner = new Scanner(System.in);
            this.inTextFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream())); //Read data from server
            this.outTextToServer = new PrintWriter(clientSocket.getOutputStream(),true); //used to send the data to server -> println()
            this.inByteFromServer = new BufferedInputStream(clientSocket.getInputStream()); 
            //Loop run
            String command = "";
            while(!clientSocket.isClosed()){
                System.out.println("----------------------------------------");
                System.out.println("-- All Command --");
                System.out.println("Type /filelist to see file list in server");
                System.out.println("Type /request to download file from server");
                System.out.println("Type /exit to exit the program");
                System.out.println("Enter your command : ");
                
                command = scanner.nextLine(); //type text from keyboard
                switch (command) {
                    case "/filelist":
                        //method filelist
                        outTextToServer.println(command); // send command to server
                        String allFilesInServer;
                        while( (allFilesInServer = inTextFromServer.readLine() ) != null ){
                            if ("EOF".equals(allFilesInServer)) {
                                break;
                            }
                            System.out.println(allFilesInServer);
                        }
                        break;
                    case "/request":
                        
                        //method download file
                        outTextToServer.println(command); // send command to server "request"
                        
                        System.out.print("Enter specific filename that you want to download: ");
                        String filename = scanner.nextLine();
                        outTextToServer.println(filename); //send filename to server
                        
                        System.out.println("Type '1' or '2' to choose what type you want");
                        System.out.println("[1] NormalCopy");
                        System.out.println("[2] ZeroCopy");
                        //send message type of copy [Normal or ZeroCopy]
                        System.out.print("Enter Type: ");
                        String type = scanner.nextLine();
//                        if(!type.equals("1") || !type.equals("2")){
//                            System.err.println("Please Enter 1 or 2 ONLY!! ");
//                            break;
//                        }
                        outTextToServer.println(type);
                         
                        String messageFromServer = inTextFromServer.readLine(); //receive Long/String from server
                        long fileSize = 0;
                        String errorFromServer = "";
                        if(messageFromServer.equals("Long")){
                            fileSize = Long.parseLong(inTextFromServer.readLine());
                        }
                        else if(messageFromServer.equals("String")){
                            errorFromServer = inTextFromServer.readLine();
                        }
                         
                        if(fileSize > 0){
//                            System.out.println("FileSize : " + fileSize);
                            if(type.equals("1")){
                                System.out.println("> " + clientSocket + " -> NormalCopy");
                                long start = System.currentTimeMillis();
                                //method sendNormal
                                copy(filename, fileSize);
                                long end = System.currentTimeMillis();
                                long time = end - start;
                                System.out.println("> Time " + time + " millisecond [NormalCopy] File: " + filename);
                            }
                            else if(type.equals("2")){
                                System.out.println("> " + clientSocket + " -> ZeroCopy");
                                long start = System.currentTimeMillis();
                                //method sendZeroCopy
                                zeroCopy(filename, fileSize);
                                long end = System.currentTimeMillis();
                                long time = end - start;
                                System.out.println("> Time " + time + " millisecond [ZeroCopy] File: " + filename);
                            }
                            else{
                                System.out.println("Enter 1 or 2 Only! , Please try again");
                                break;
                            }
                        }
                        else{
                            System.err.println("Server said : " + errorFromServer);
                        }
                        break;
                    case "/exit":
                        outTextToServer.println(command);
                        close();
                        break;
                    default:
                        System.out.println("Wrong command , Please try again");
                        break;
                }
            }
        } catch (IOException e) {
            System.err.println("Error from Client()");
            e.printStackTrace();
        }
        finally{
//            System.out.println("STATUS Connect: " + clientSocket.isConnected());
//            System.out.println("STATUS Close: " + clientSocket.isClosed());
//            if (clientSocket.isClosed()) {
//                System.out.println("Connection closed by the server. STATUS: " + clientSocket.isConnected());
//            }
//            else{
//                System.out.println(clientSocket + " --> OK! , status: " + clientSocket.isConnected());
//            }
////            close();
        }
    }
    
    private void close() {
        try {
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
                System.out.println("Client socket closed.");
            }
            clientSocketChannel.close();
            inTextFromServer.close();
            outTextToServer.close();
            inByteFromServer.close();
        } catch (IOException e) {
            System.err.println("Error closing client socket: " + e.getMessage());
        }
    }
    
    public void copy(String filename,long fileSize){
        try(FileOutputStream fileToDisk = new FileOutputStream(CLIENT_FILEPATH + filename) ){
            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalBytesRead = 0;

            System.out.println("fileSize : " + fileSize);
            System.out.println("Starting file download...");
            while ( totalBytesRead < fileSize && (bytesRead = inByteFromServer.read(buffer)) != -1) {
                if (totalBytesRead + bytesRead > fileSize) {
                    bytesRead = (int)(fileSize - totalBytesRead);
                } 
                totalBytesRead += bytesRead;
                System.out.println("Bytes read: " + bytesRead + ", Total bytes read: " + totalBytesRead);
                fileToDisk.write(buffer, 0, bytesRead);

            }
            System.out.println("Expected fileSize : " + fileSize);
            System.out.println("Final total bytes read: " + totalBytesRead);
            if (totalBytesRead != fileSize) {
                System.err.println("Warning: File size mismatch. Expected: " + fileSize + ", but got: " + totalBytesRead);
            } else {
                System.out.println("File download complete!");
            }
        }
        catch(IOException e){
            e.printStackTrace();
        }      
    }
    
    public void zeroCopy(String filename, long fileSize){
        try(FileChannel destinationFile = new FileOutputStream(CLIENT_FILEPATH + filename).getChannel() ){
            long position = 0;
            long count;
            //fileSize - position
            while (position < fileSize && (count = destinationFile.transferFrom(clientSocketChannel, position, fileSize - position)) > 0) {
                position += count;
                System.out.println("Transferred: " + count + " bytes, Total transferred: " + position + " bytes");
            }
            
            if (position == fileSize) {
                System.out.println("File download complete!");
            } else {
                System.err.println("Warning: Expected file size was " + fileSize + " bytes, but only " + position + " bytes were transferred.");
            }
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }

    
    public static void main(String[] args) {
        new Client();
    }

}
