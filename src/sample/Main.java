package sample;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import java.io.*;
import java.net.InetAddress;

public class Main extends Application {

    File chosenFilesPath;
    @Override
    public void start(Stage primaryStage) throws Exception{
        primaryStage.setTitle("Slava Image Transfer Server");

        chosenFilesPath = new File("D:/SLAVA/Images");


        Label chosenPathLabel = new Label ("Chosen Path: ");
        Label chosenPathValue = new Label (chosenFilesPath.toString());
        HBox chosenPathBox = new HBox();
        chosenPathBox.getChildren().add(chosenPathLabel);
        chosenPathBox.getChildren().add(chosenPathValue);
        chosenPathBox.setAlignment(Pos.CENTER);

        Button chooseFilePathButton = new Button("Choose Files Path");

        Label ipAddressLabel = new Label ("Your IP-Address: ");
        Label ipAddressValue = new Label ("");
        HBox ipAddressBox = new HBox();
        ipAddressBox.getChildren().add(ipAddressLabel);
        ipAddressBox.getChildren().add(ipAddressValue);
        ipAddressBox.setAlignment(Pos.CENTER);

        Button checkIpAdressButton = new Button("Check IP-Address");


        VBox mainVBox = new VBox();
        mainVBox.getChildren().add(chosenPathBox);
        mainVBox.getChildren().add(chooseFilePathButton);
        mainVBox.getChildren().add(ipAddressBox);
        mainVBox.getChildren().add(checkIpAdressButton);
        mainVBox.setAlignment(Pos.CENTER);

        StackPane stackPane = new StackPane();
        BackgroundImage myBI= new BackgroundImage(new Image("file:///D:/UNIVER/KPP/KursWork/Server2/src/sample/for_PC.png", 1920,1080, true, true, true),
                BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER,
                BackgroundSize.DEFAULT);

        stackPane.setBackground(new Background(myBI));
        stackPane.getChildren().add(mainVBox);
        primaryStage.setScene(new Scene(stackPane, 1000, 700));

        SocketChecker socketCheckerThread = new SocketChecker(chosenFilesPath);
        chooseFilePathButton.setOnAction(new EventHandler<ActionEvent>(){
            public void handle(ActionEvent event) {
                DirectoryChooser directoryChooser = new DirectoryChooser();
                directoryChooser.setTitle("Choose direction to reserve coping");
                if (chosenFilesPath.exists())
                    directoryChooser.setInitialDirectory(chosenFilesPath);
                else directoryChooser.setInitialDirectory(new File("C:"));

                File filePath = directoryChooser.showDialog(primaryStage);
                if (filePath != null)
                    if (!filePath.exists() && filePath.getParentFile().exists())
                        filePath = filePath.getParentFile();
                if (filePath.exists()) chosenFilesPath = filePath;;
                chosenPathValue.setText(chosenFilesPath.toString());
                socketCheckerThread.setChoosenFilesPath(chosenFilesPath);
            }
        });
        checkIpAdressButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                try {
                    ipAddressValue.setText(InetAddress.getLocalHost().getHostAddress());
                }catch (Exception ex){
                    System.out.println("Exception_in_checkIp");
                    ipAddressValue.setText("Exception");
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
