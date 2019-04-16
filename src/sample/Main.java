package sample;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import java.io.*;
import java.net.InetAddress;

public class Main extends Application {

    File choosenFilesPath;
    private BufferedReader br;
    private InputStreamReader isr;
    private String message = "";
    private Label messageValue;

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

        StackPane stackPane = new StackPane();
        stackPane.getChildren().add(mainVBox);
        primaryStage.setScene(new Scene(stackPane, 600, 400));

        SocketChecker socketCheckerThread = new SocketChecker(choosenFilesPath);
        chooseFilePathButton.setOnAction(new EventHandler<ActionEvent>(){
            public void handle(ActionEvent event) {
                DirectoryChooser directoryChooser = new DirectoryChooser();
                directoryChooser.setTitle("Choose direction to reserv coping");
                if (choosenFilesPath.exists())
                    directoryChooser.setInitialDirectory(choosenFilesPath);
                else directoryChooser.setInitialDirectory(new File("C:"));

                File filePath = directoryChooser.showDialog(primaryStage);
                if (filePath != null)
                    if (!filePath.exists() && filePath.getParentFile().exists())
                        filePath = filePath.getParentFile();
                if (filePath.exists()) choosenFilesPath = filePath;;
                choosenPathValue.setText(choosenFilesPath.toString());
                socketCheckerThread.setChoosenFilesPath(choosenFilesPath);
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
        primaryStage.show();

        socketCheckerThread.start();
        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                socketCheckerThread.interrupt();
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
