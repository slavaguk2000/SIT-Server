package sample;

import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.*;
import java.lang.reflect.Array;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.EventObject;
import java.util.Vector;

public class Main extends Application {

    File choosenFilesPath;

    private ServerSocket ss;
    private Socket imageSocket;
    private BufferedReader br;
    private InputStreamReader isr;
    private String message = "";
    Label messageValue;

    @Override
    public void start(Stage primaryStage) throws Exception{
        primaryStage.setTitle("Server");

        choosenFilesPath = new File("D:/SLAVA/Images");


        Label choosenPathLabel = new Label ("Choosen Path: ");
        Label choosenPathValue = new Label (choosenFilesPath.toString());
        HBox choosenPathBox = new HBox();
        choosenPathBox.getChildren().add(choosenPathLabel);
        choosenPathBox.getChildren().add(choosenPathValue);
        choosenPathBox.setAlignment(Pos.CENTER);

        Button chooseFilePathButton = new Button("Choose Files Path");

        Label ipAdressLabel = new Label ("Your IP-Adress: ");
        Label ipAdressValue = new Label ("");
        HBox ipAdressBox = new HBox();
        ipAdressBox.getChildren().add(ipAdressLabel);
        ipAdressBox.getChildren().add(ipAdressValue);
        ipAdressBox.setAlignment(Pos.CENTER);

        Button checkIpAdressButton = new Button("Check IP-Adress");

        Label messageLabel = new Label ("Your message: ");
        messageValue = new Label (".");
        HBox messageBox = new HBox();
        messageBox.getChildren().add(messageLabel);
        messageBox.getChildren().add(messageValue);
        messageBox.setAlignment(Pos.CENTER);

        ImageView worldImage = new ImageView(new File("D:/UNIVER/KursWork/267px-Rotating_earth_(large).gif").toURI().toURL().toString());

        VBox mainVBox = new VBox();
        mainVBox.getChildren().add(choosenPathBox);
        mainVBox.getChildren().add(chooseFilePathButton);
        mainVBox.getChildren().add(ipAdressBox);
        mainVBox.getChildren().add(checkIpAdressButton);
        mainVBox.getChildren().add(messageBox);
        mainVBox.setAlignment(Pos.CENTER);
        // mainVBox.getChildren().add(worldImage);


        StackPane stackPane = new StackPane();
        stackPane.getChildren().add(mainVBox);
        primaryStage.setScene(new Scene(stackPane, 600, 400));


        chooseFilePathButton.setOnAction(new EventHandler<ActionEvent>(){
            public void handle(ActionEvent event) {
                DirectoryChooser directoryChooser = new DirectoryChooser();
                directoryChooser.setTitle("Choose direction to reserv coping");
                if (choosenFilesPath.exists())
                    directoryChooser.setInitialDirectory(choosenFilesPath);
                else directoryChooser.setInitialDirectory(new File("C:"));
                do {
                    choosenFilesPath = directoryChooser.showDialog(primaryStage);
                    if (!choosenFilesPath.exists() && choosenFilesPath.getParentFile().exists())
                        choosenFilesPath = choosenFilesPath.getParentFile();
                }while (!choosenFilesPath.exists());
                choosenPathValue.setText(choosenFilesPath.toString());
            }
        });
        checkIpAdressButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                try {
                    ipAdressValue.setText(InetAddress.getLocalHost().getHostAddress().toString());
                }catch (Exception ex){
                    System.out.println("Exception_in_checkIp");
                    ipAdressValue.setText("Exception");
                }
            }
        });

        Thread socketCheckerThread = new Thread(new SocketChecker());
        socketCheckerThread.start();

        primaryStage.show();
    }
    public void setMessageText(String mes)    {
        messageValue.setText(mes);
    }

    class SocketChecker implements Runnable{

        private void checkDiviseIsFirst(String clientDeviceName, String homeDirectory, DataOutputStream socketWriter) throws IOException {
            final Vector<String> existFiles = new Vector<>();
            boolean isFirstTime = true;
            for (File includingDir : choosenFilesPath.listFiles()) {
                if (includingDir.isDirectory()) {
                    String[] dirName = includingDir.getName().split("@", 2);
                    if (dirName.length == 2)
                        if (dirName[1].equals(clientDeviceName)) {
                            includingDir.renameTo(new File(homeDirectory));
                            isFirstTime = false;
                            try {
                                Files.walk(Paths.get(choosenFilesPath.toString())).filter(Files::isRegularFile).
                                        forEach(string -> existFiles.add(string.toString().replace(homeDirectory, "")));
                            } catch (IOException ex) {
                            }
                            break;
                        }
                }
            }
            socketWriter.writeInt(existFiles.size());
            for (String str : existFiles) {
                socketWriter.writeUTF(str);
            }
            socketWriter.flush();
        }
        @Override
        public void run() {
            FileOutputStream fileWriter = null;
            try {
                ss = new ServerSocket(2154);
                System.out.println("Server run at port 2154");
                imageSocket = ss.accept();
                DataInputStream socketReader = new DataInputStream(imageSocket.getInputStream());
                DataOutputStream socketWriter = new DataOutputStream(imageSocket.getOutputStream());
                int bufferSize = 64*1024;

                String clientAdress = imageSocket.getInetAddress().toString();
                String clientDeviceName = socketReader.readUTF();
                String clientName = socketReader.readUTF();
                String homeDirectory = new File(choosenFilesPath.toString() + "/" + clientName + "@" + clientDeviceName).toString();
                checkDiviseIsFirst(clientDeviceName, homeDirectory, socketWriter);
                while(true) {
                    long fileLength = socketReader.readLong();
                    String filePath = socketReader.readUTF();
                    byte[] readBuffer = new byte[bufferSize];
                    if (filePath == "null" && fileLength == 0) break;
                    String newFilePath = homeDirectory + filePath;
                    File image = new File(newFilePath);
                    image.getParentFile().mkdirs();
                    fileWriter = new FileOutputStream(newFilePath);
                    int count, total = 0;
                    while ((count = socketReader.read(readBuffer, 0, (int)Math.min(readBuffer.length, fileLength-total))) != -1){
                        total += count;
                        fileWriter.write(readBuffer, 0, count);

                        if(total == fileLength){
                            break;
                        }
                    }

                    fileWriter.flush();
                    fileWriter.close();

                }

            } catch(IOException ex){
                System.out.println(ex.fillInStackTrace());
            }
            catch (Exception ex){
                System.out.println("Something else: " + ex.getMessage());
            }

        }
    }

    class SocetCheck extends Task<Void>{
        @Override
        protected void finalize() throws Throwable {
            super.finalize();
        }
        @Override
        protected Void call()        {
//            try {
//                //while(true) {
//                    ss = new ServerSocket(5000);
//                    System.out.println("Server run at port 5000");
//                    s = ss.accept();
//
//                    isr = new InputStreamReader(s.getInputStream());
//                    br = new BufferedReader(isr);
//                    message = br.readLine();
//
//                    System.out.println(message);
//                    //setMessageText(message);
//
//                    isr.close();
//                    br.close();
//                    ss.close();
//                    s.close();
//               // }
//            } catch(IOException ex){
//                System.out.println("Exception_in_socketChecker");
//                //ex.printStackTrace();
//            }
//            catch (Exception ex){
//                System.out.println("Something else: " + ex.getMessage());
//            }
            return null;
        }
    }


    public static void main(String[] args) {
        launch(args);
    }
}
