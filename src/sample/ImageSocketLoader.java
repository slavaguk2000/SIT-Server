package sample;

import javafx.application.Platform;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

public class ImageSocketLoader extends Thread{
    File choosenFilesPath;
    Socket imageSocket;
    String clientName = "Unknown";
    String clientDeviceName;
    short numberOfLoading;
    String homeDirectory;
    public ImageSocketLoader(Socket imageSocket, File choosenFilesPath, short numberOfLoading){
        super();
        this.choosenFilesPath = choosenFilesPath;
        this.imageSocket = imageSocket;
        this.numberOfLoading = numberOfLoading;
    }

    @Override
    public void run() {
        try {
            DataInputStream socketReader = new DataInputStream(imageSocket.getInputStream());
            DataOutputStream socketWriter = new DataOutputStream(imageSocket.getOutputStream());

            clientDeviceName = socketReader.readUTF();
            clientName = socketReader.readUTF();
            boolean isSend = socketReader.readBoolean();
            if (isSend) sendImages(socketReader, socketWriter, clientDeviceName);
            else downloadImages(socketReader, socketWriter, clientDeviceName);
            imageSocket.close();
        }catch (IOException ex){
            System.out.println("IOException in " + numberOfLoading + " loading with \"" + clientName + "\"(" + imageSocket.getInetAddress() + "): " + ex.getMessage());
        }catch (Exception ex){
            System.out.println("Exception in " + numberOfLoading + " loading: " + ex.getMessage());
        }

    }

    private void sendImages(DataInputStream socketReader, DataOutputStream socketWriter, String clientDeviceName) throws Exception{
        FileOutputStream fileWriter;
        homeDirectory = new File(choosenFilesPath.toString() + "/" + clientName + "@" + clientDeviceName).toString();
        checkDiviseIsFirst(clientDeviceName, socketWriter);
        final int countOfImage = socketReader.readInt();
        int currentImage = 0;
        ProgressBarDialog progressBar = new ProgressBarDialog("from " + clientName);
        Platform.runLater(progressBar);
        int endProgress = 100;
        while (true) {
            try {
                final int finalCurrentImage = currentImage;
                Platform.runLater(() -> {
                    progressBar.setProgress(100 * finalCurrentImage / countOfImage);
                });
                currentImage++;
                long fileLength = socketReader.readLong();
                String filePath = socketReader.readUTF();
                if (filePath.equals("exist") && fileLength == 1) continue;
                byte[] readBuffer = new byte[64 * 1024];
                if ((filePath.equals("null") && fileLength == 0) || isInterrupted() || !progressBar.isOpen) break;
                if ((filePath.equals("null") && fileLength == 1)) {
                    endProgress = -1;
                    break;
                }
                String newFilePath = homeDirectory + filePath;
                File image = new File(newFilePath);
                image.getParentFile().mkdirs();
                fileWriter = new FileOutputStream(newFilePath);
                int count, total = 0;
                while ((count = socketReader.read(readBuffer, 0, (int) Math.min(readBuffer.length, fileLength - total))) != -1) {
                    total += count;
                    fileWriter.write(readBuffer, 0, count);

                    if (total == fileLength) {
                        break;
                    }
                }
                fileWriter.flush();
                fileWriter.close();
            }catch (Exception ex){
                Platform.runLater(()->{ progressBar.setProgress(-1);});
                throw ex;
            }
        }
        int finalRndProgress = endProgress;
        Platform.runLater(()->{ progressBar.setProgress(finalRndProgress);});
    }

    private void downloadImages(DataInputStream socketReader, DataOutputStream socketWriter, String clientDeviceName) throws Exception{
        int readCount;
        File currentImage;
        FileInputStream fileReader;
        Vector<String> imagesFromPreviousCopy = getImagesPaths(socketWriter);
        if (imagesFromPreviousCopy== null) return;
        int countOfImage = imagesFromPreviousCopy.size();

        ///// Logic of write
        socketWriter.writeInt(countOfImage);
        for (int i = 0; i < countOfImage; i++){
            socketWriter.writeUTF(imagesFromPreviousCopy.get(i).replace(homeDirectory, ""));
        }

        Vector<Integer> numbersOfNeeds = new Vector<>();
        int countOfNeeds = socketReader.readInt();
        for (int i = 0; i < countOfNeeds; i++){
            numbersOfNeeds.add(socketReader.readInt());
        }
        ProgressBarDialog progressBar = new ProgressBarDialog("from " + clientName);
        Platform.runLater(progressBar);
        try {
            for (int i = 0; i < countOfNeeds; i++) {
                String currentImagePath = imagesFromPreviousCopy.get(numbersOfNeeds.get(i));
                currentImage = new File(currentImagePath);
                long fileLength = currentImage.length();
                socketWriter.writeLong(fileLength);
                socketWriter.writeUTF(currentImagePath.replace(homeDirectory, ""));
                fileReader = new FileInputStream(currentImage);
                byte[] sendBuffer = new byte[64 * 1024];
                while ((readCount = fileReader.read(sendBuffer)) != -1) {
                    socketWriter.write(sendBuffer, 0, readCount);
                }
                socketWriter.flush();
                fileReader.close();
                final int finalCurrentImage = i;
                Platform.runLater(()->{ progressBar.setProgress(100*finalCurrentImage/countOfNeeds);});
            }
        }catch(Exception ex){}
        Platform.runLater(()->{ progressBar.setProgress(1);});
        try {
            wait(5000);
        }catch(Exception ex){}
    }

    private Vector<String> getImagesPaths(DataOutputStream socketWriter)throws IOException{
        final Vector<String> existFiles = new Vector<>();
        final TreeSet<String> names = new TreeSet<>();
        String[] clientAttributes = {clientName, clientDeviceName};
        for(short attribute = 0; attribute < 2; attribute++) {
            for (File includingDir : choosenFilesPath.listFiles()) {
                if (includingDir.isDirectory()) {
                    String[] dirName = includingDir.getName().split("@", 2);
                    if (dirName.length == 2){
                        if (dirName[attribute].equals(clientAttributes[attribute])) {
                            try {
                                Files.walk(Paths.get(choosenFilesPath.toString())).filter(Files::isRegularFile).
                                        forEach(string -> existFiles.add(string.toString()));
                            } catch (IOException ex) {}
                            homeDirectory = includingDir.toString();
                            break;
                        }
                        if (attribute == 0) names.add(dirName[0]);
                    }
                }
            }
            if(existFiles.size() > 0) break;
        }
        if(existFiles.size() > 0) {
            socketWriter.writeBoolean(true);//есть такое сохранение
            return existFiles;
        }else{
            socketWriter.writeBoolean(false);//такого сохранения нет
            socketWriter.writeInt(names.size());//количество сохранений
            for (String name: names) {
                socketWriter.writeUTF(name);
            }//передача сохранений
            return null;////////////////объединить с предыдущим обходои, переписать логику
        }
    }

    private void checkDiviseIsFirst(String clientDeviceName, DataOutputStream socketWriter) throws IOException {
        final Vector<String> existFiles = new Vector<>();
        for (File includingDir : choosenFilesPath.listFiles()) {
            if (includingDir.isDirectory()) {
                String[] dirName = includingDir.getName().split("@", 2);
                if (dirName.length == 2)
                    if (dirName[1].equals(clientDeviceName)) {
                        includingDir.renameTo(new File(homeDirectory));
                        try {
                            Files.walk(Paths.get(choosenFilesPath.toString())).filter(Files::isRegularFile).
                                    forEach(string -> existFiles.add(string.toString().replace(homeDirectory, "")));
                        } catch (IOException ex) {}
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
}
