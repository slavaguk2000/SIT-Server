package sample;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Vector;

public class ImageSocketLoader extends Thread{
    File choosenFilesPath;
    Socket imageSocket;
    String clientName = "Unknown";
    short numberOfLoading;
    public ImageSocketLoader(Socket imageSocket, File choosenFilesPath, short numberOfLoading){
        super();
        this.choosenFilesPath = choosenFilesPath;
        this.imageSocket = imageSocket;
        this.numberOfLoading = numberOfLoading;
    }

    @Override
    public void run() {
        try {
            FileOutputStream fileWriter = null;
            DataInputStream socketReader = new DataInputStream(imageSocket.getInputStream());
            DataOutputStream socketWriter = new DataOutputStream(imageSocket.getOutputStream());
            int bufferSize = 64 * 1024;

            String clientDeviceName = socketReader.readUTF();
            String clientName = socketReader.readUTF();
            int countOfImage = socketReader.readInt();
            if(clientName != null) this.clientName = clientName;
            String homeDirectory = new File(choosenFilesPath.toString() + "/" + clientName + "@" + clientDeviceName).toString();
            checkDiviseIsFirst(clientDeviceName, homeDirectory, socketWriter);
            while (true) {
                long fileLength = socketReader.readLong();
                String filePath = socketReader.readUTF();
                byte[] readBuffer = new byte[bufferSize];
                if ((filePath.equals("null") && fileLength == 0) || isInterrupted()) break;
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

            }
            imageSocket.close();
        }catch (IOException ex){
            System.out.println("IOException in " + numberOfLoading + " loading with \"" + clientName + "\"(" + imageSocket.getInetAddress() + "): " + ex.getMessage());
        }catch (Exception ex){
            System.out.println("Exception in " + numberOfLoading + " loading: " + ex.getMessage());
        }

    }

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
}
