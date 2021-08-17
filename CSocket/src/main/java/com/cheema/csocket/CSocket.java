package com.cheema.csocket;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import io.paperdb.Paper;

import static android.content.Context.MODE_PRIVATE;
import static android.content.Context.WIFI_SERVICE;

public class CSocket {

    //variables for connecting with server
    private Socket server;
    private PrintWriter printWriter;
    private BufferedReader bufferedReader;
    private String serverIp = "";
    private int serverPort;
    private Context context;
    private String uniqueIdentifier;
    CInterface cInterface;

    private final String preferencesName = "com.cheema.socket";
    private final String TAG = "com.cheema.socket";
    private final String portString = "serverPort";
    private final String serverIpString = "serverIp";
    private final String clientPortString = "clientPort";
    private final String devicesTable = "devicesTable";

    //variables related for receiving data from server without any request
    private ServerSocket serverSocket = null;
    private boolean isStopped = false;
    private int clientPort;

    //constructor
    public CSocket(Context context, CInterface cInterface, String uniqueIdentifier) {
        Paper.init(context);
        this.context = context;
        this.cInterface = cInterface;
        this.uniqueIdentifier = uniqueIdentifier;
        this.clientPort = context.getSharedPreferences(preferencesName, MODE_PRIVATE).getInt(clientPortString, 1024);
    }

    public void setServerConfig(String serverIp, int serverPort) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
    }

    public String getServerIp() {
        return serverIp;
    }

    public void setServerIp(String serverIp) {
        this.serverIp = serverIp;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    /* ********************************************************* Client Related ************************************************* */
    /* ********************************************************* Client Related ************************************************* */
    /* ********************************************************* Client Related ************************************************* */

    //method to send data to server
    public void sendData(String data) {
        DataPacket dataPacket = new DataPacket(getLocalIpAddress(), clientPort, data, uniqueIdentifier);
        sendDataToServer(new Gson().toJson(dataPacket));
    }

    public void sendData(Object data) {
        DataPacket dataPacket = new DataPacket(getLocalIpAddress(), clientPort, new Gson().toJson(data), uniqueIdentifier);
        sendDataToServer(new Gson().toJson(dataPacket));
    }

    private void sendDataToServer(String data) {
        if (serverSocket == null) {
            startServer();
        }
        new SenderAsyncTask(data, serverIp, serverPort).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private class SenderAsyncTask extends AsyncTask<Void, Void, Void> {
        private PrintWriter printWriter = null;
        private String ip;
        private int port;
        String data;

        public SenderAsyncTask(String data, String ip, int port) {
            Log.e(TAG, "SenderAsyncTask");
            this.ip = ip;
            this.port = port;
            this.data = data;
            Log.e(TAG, "SenderAsyncTask => " + "socket ip: " + ip + ", port = " + port);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                Log.e(TAG, "SenderAsyncTask => run =>" + ip + ":" + port);
                server = new java.net.Socket(ip, port); // Creating the server socket.
                Log.e(TAG, "SenderAsyncTask => run => " + "Connected to server");
                printWriter = new PrintWriter(server.getOutputStream(), true);
                bufferedReader = new BufferedReader(new InputStreamReader(server.getInputStream()));
                printWriter.write(data);
                printWriter.flush();
                server.shutdownOutput();
                sendDataToUI(data);
                new Receiver().run();
            } catch (Exception e) {
                server = null;
                bufferedReader = null;
                printWriter = null;
                String message = "Connection failed with server";
                Log.e(TAG, "SenderAsyncTask => run => " + message);
                if (cInterface != null)
                    cInterface.onFailure(message);
                Log.e(TAG, message);
                e.printStackTrace();
            }
            return null;
        }

        //method to receive data from server
        class Receiver implements Runnable {
            @Override
            public void run() {
                boolean flag = true;
                try {
                    while (flag) {
                        if (server != null && bufferedReader != null) {
                            final String message = bufferedReader.readLine();
                            Log.e(TAG, "Response from server = " + message);
                            if (cInterface != null && message != null) {
                                cInterface.onDataReceived(message, null);
                            }
                            bufferedReader.close();
                            server.close();
                            bufferedReader = null;
                            server = null;
                        } else {
                            Log.e(TAG, "closing receiver");
                            flag = false;
                        }
                    }
                    if (bufferedReader != null) {
                        Log.e(TAG, "closing bufferedReader");
                        bufferedReader.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //send response back to ui
    private void sendDataToUI(String data) {

        Gson gson = new Gson();
        DataPacket dataPacket = gson.fromJson(data, DataPacket.class);

        //convert object string to object to get original data
        if (cInterface != null)
            cInterface.onDataSent(dataPacket.getClientData().toString());
    }


    /* ********************************************************* Server Related ************************************************* */
    /* ********************************************************* Server Related ************************************************* */
    /* ********************************************************* Server Related ************************************************* */

    //start server to accept new connections
    public void startServer() {
        new ServerAsyncTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    //start server to accept new connections on specific port
    public void startServer(int port) {
        if (port < 1024 && port > 65535) {
            String error = "Port range is 1024 to 65535";
            Log.e(TAG, "startServer => error = " + error);
            sendDataToUI(error);
        } else {
            this.clientPort = port;
            if (context != null) {
                context.getSharedPreferences(preferencesName, MODE_PRIVATE).edit().putInt(clientPortString, port).apply();
            }
            new ServerAsyncTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    //async task to open socket to accept any new connections
    private class ServerAsyncTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                if (serverSocket != null) {
                    serverSocket.close();
                }
                serverSocket = new ServerSocket(clientPort);
                Log.e(TAG, " ServerAsyncTask");
                while (!isStopped) {
                    Socket clientSocket = null;
                    try {
                        clientSocket = serverSocket.accept();
                        new Thread(new WorkerRunnable(context, clientSocket)).start();
                        Log.e(TAG, " ServerAsyncTask => accept new connection");
                    } catch (Exception e) {
                        if (isStopped) {
                            Log.e(TAG, "ServerAsyncTask - Server Stopped.");
                        }
                    }
                }
            } catch (Exception e) {
                isStopped = true;
                Log.e(TAG, " ServerAsyncTask => Cannot start server on port = " + clientPort + ". Reason = " + e);
            }
            return null;
        }
    }

    public void sendDataToClient(String data, String uniqueIdentifier) {
        String completeClientIp = getClientIp(uniqueIdentifier);
        if (completeClientIp != null) {
            String[] array = completeClientIp.split(":");
            if (array.length == 2) {
                String ip = array[0];
                int port = Integer.parseInt(array[1]);
                new SenderAsyncTask(data, ip, port).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            } else {
                Log.e(TAG, "sendDataToClient => client not found");
            }
        } else {
            Log.e(TAG, "sendDataToClient => client not found");
        }
    }

    //class to receive data sent from sever
    private class WorkerRunnable implements Runnable {

        protected Socket clientSocket;
        protected Context context;
        private final String TAG = "WorkerRunnable";

        public WorkerRunnable(Context context, Socket clientSocket) {
            this.clientSocket = clientSocket;
            this.context = context;
        }

        @Override
        public void run() {
            if (clientSocket != null) {
                try {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    PrintWriter printWriter = new PrintWriter(clientSocket.getOutputStream(), true);
                    String dataString = bufferedReader.readLine();
                    Log.e(TAG, "run => Message from client = " + dataString);
                    processClientData(dataString, printWriter);
                    printWriter.write("ok");
                    printWriter.flush();
                    printWriter.close();
                    bufferedReader.close();
                    clientSocket.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        private void processClientData(String dataString, PrintWriter printWriter) {
            try {
                Gson gson = new Gson();
                DataPacket dataPacket = gson.fromJson(dataString, DataPacket.class);

                if (cInterface != null)
                    cInterface.onDataReceived(dataPacket.getClientData().toString(), printWriter);

                Paper.book(devicesTable).write(dataPacket.uniqueIdentifier, dataPacket.clientAddress + ":" + dataPacket.getClientPort());
            } catch (Exception ex) {
                if (cInterface != null)
                    cInterface.onDataReceived(dataString, printWriter);
                ex.printStackTrace();
            }
        }
    }

    /* ********************************************************* Helpers ************************************************* */
    /* ********************************************************* Helpers ************************************************* */
    /* ********************************************************* Helpers ************************************************* */
    private String convertToString(DataPacket myObj) {
        Gson gson = new Gson();
        String data = gson.toJson(myObj);
        try {
            data = new String(data.getBytes(), "UTF-8");
//            data = "{\"clientAddress\": \"" + myObj.getClientAddress() + "\",\"clientData\": \"" + addForwardSlash(data)
            data = "{\"clientAddress\": \"" + myObj.getClientAddress() + "\",\"clientData\": \"" + myObj.getClientData()
                    + "\",\"clientPort\": \"" + myObj.getClientPort()
                    + "\",\"uniqueIdentifier\": \"" + myObj.getUniqueIdentifier()
                    + "\"}";
            Log.e(TAG, "convertToString: " + data);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
        return data;
    }

    //get client ip against identifier
    private String getClientIp(String uniqueIdentifier) {
        return Paper.book(devicesTable).read(uniqueIdentifier);
    }

    //get device ip
    private String getLocalIpAddress() {
        try {
            WifiManager wifiManager = (WifiManager) context.getSystemService(WIFI_SERVICE);
            assert wifiManager != null;
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int ipInt = wifiInfo.getIpAddress();
            return InetAddress.getByAddress(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ipInt).array()).getHostAddress();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    //class used to transfer data between client and server
    private static class DataPacket {
        @SerializedName("clientAddress")
        @Expose
        private String clientAddress;
        @SerializedName("clientPort")
        @Expose
        private int clientPort;
        @SerializedName("clientData")
        @Expose
        private String clientData;
        @SerializedName("uniqueIdentifier")
        @Expose
        private String uniqueIdentifier;

        public DataPacket(String clientAddress, int clientPort, String clientData, String uniqueIdentifier) {
            this.clientAddress = clientAddress;
            this.clientPort = clientPort;
            this.clientData = clientData;
            this.uniqueIdentifier = uniqueIdentifier;
        }

        public String getUniqueIdentifier() {
            return uniqueIdentifier;
        }

        public void setUniqueIdentifier(String uniqueIdentifier) {
            this.uniqueIdentifier = uniqueIdentifier;
        }

        public String getClientAddress() {
            return clientAddress;
        }

        public void setClientAddress(String clientAddress) {
            this.clientAddress = clientAddress;
        }

        public int getClientPort() {
            return clientPort;
        }

        public void setClientPort(int clientPort) {
            this.clientPort = clientPort;
        }

        public Object getClientData() {
            return clientData;
        }

        public void setClientData(String clientData) {
            this.clientData = clientData;
        }
    }
}