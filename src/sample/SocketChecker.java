package sample;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

public class SocketChecker extends Thread{
    File choosenFilesPath;
    public SocketChecker (File choosenFilesPath){
        super();
        this.choosenFilesPath = choosenFilesPath;
    }

    public void setChoosenFilesPath(File choosenFilesPath) {
        this.choosenFilesPath = choosenFilesPath;
    }

    @Override
    public void run() {
        try {
            ServerSocket mainServerSocket = null;
            short numberOfConnections = 0;
            ArrayList<Thread> loaders = new ArrayList<>();
            mainServerSocket = new ServerSocket(2154);
            System.out.println("Server run at port 2154");
            while(true) {
                try {
                    if (isInterrupted()) break;
                    mainServerSocket.setSoTimeout(5000);
                    while (true) {
                        numberOfConnections++;
                        Socket imageSocket = mainServerSocket.accept();
                        System.out.println("Client with IP:" + imageSocket.getInetAddress().toString() + " was connected");
                        ImageSocketLoader imageSocketLoader = new ImageSocketLoader(imageSocket, choosenFilesPath, numberOfConnections);
                        imageSocketLoader.start();
                        loaders.add(imageSocketLoader);
                    }
                }catch (SocketTimeoutException ex){
                    if (isInterrupted()) {
                        if (mainServerSocket != null) mainServerSocket.close();
                        break;
                    }
                }
            }
            for (Thread thread: loaders) {
                thread.interrupt();
            }
        } catch(IOException ex){
            System.out.println(ex.fillInStackTrace());
        }
        catch (Exception ex){
            System.out.println("Something else: " + ex.getMessage());
        }

    }
}
