package sample;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

public class ProgressBarDialog implements Runnable {
    public boolean isOpen = true;
    Label informationLabel;
    Button cancelButton;
    Label titleLabel;
    private String information;
    public ProgressBarDialog(String name){
        information = "Copying files from " + name + ": ";
    }

    private Stage dialogStage;
    private final ProgressBar progressBar = new ProgressBar();
    private final ProgressIndicator progressIndicator = new ProgressIndicator();

    @Override
    public void run() {
        dialogStage = new Stage();
        dialogStage.initStyle(StageStyle.UTILITY);
        dialogStage.setResizable(false);
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setHeight(200);
        dialogStage.setWidth(400);

        // PROGRESS BAR
        titleLabel = new Label("Copying files...");
        informationLabel = new Label(information);
        setProgress(0);
        VBox.setMargin(titleLabel,new Insets(10,10,0,10));
        VBox.setMargin(informationLabel,new Insets(0, 10, 10,10));
        cancelButton = new Button("Cancel");
        cancelButton.setAlignment(Pos.CENTER);
        VBox.setMargin(cancelButton,new Insets(20,10,20,300));

        progressBar.setProgress(0);
        progressBar.setMinWidth(200);
        progressIndicator.setProgress(-1F);

        final HBox progressBox = new HBox();
        progressBox.setSpacing(20);
        progressBox .setAlignment(Pos.CENTER);
        progressBox .getChildren().addAll(progressBar, progressIndicator);
        final VBox mainBox = new VBox();

        mainBox.getChildren().addAll(titleLabel,informationLabel, progressBox, cancelButton);
        Scene scene = new Scene(mainBox);
        dialogStage.setScene(scene);
        dialogStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                event.consume();
            }
        });
        dialogStage.show();

        cancelButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                isOpen = false;
                dialogStage.close();
            }
        });
    }

    public void setProgress(int progress){
        informationLabel.setText(information + progress + "%");
        progressBar.setProgress(((double)progress)/100);
        if (progress == 100) {
            progressIndicator.setVisible(false);
            cancelButton.setText("Ok");
            titleLabel.setText("Copying files was completed");
        }
    }

}
