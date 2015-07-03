package eu.netide.core.shimconnectivity;

import eu.netide.core.api.IShimConnector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by timvi on 01.07.2015.
 * Socket based shim connector.
 */
public class SocketBasedShimConnector implements IShimConnector, Runnable {

    private int _port;
    private ServerSocket _socket;
    private Socket _client;
    private Thread _thread;

    public void Open(int port) {
        System.out.println("Starting shim socket server...");
        _port = port;
        _thread = new Thread(this);
        _thread.start();
    }

    public void Close() {
        try {
            System.out.println("Closing shim connection server...");
            if (_thread != null)
                _thread.interrupt();
            if (_client != null && !_client.isClosed())
                _client.close();
            if (_socket != null && !_socket.isClosed())
                _socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            _socket = new ServerSocket(_port);
            System.out.println("Waiting for shim connection on port " + _socket.getLocalPort());
            _client = _socket.accept();
            System.out.println("Shim connected (" + _client.getInetAddress().getHostAddress() + ").");
            BufferedReader in = new BufferedReader(new InputStreamReader(_client.getInputStream()));

            int byteCount;
            byte[] buffer = new byte[5 * 1024]; // a read buffer of 5KiB
            byte[] data;
            String dataText;
            while ((byteCount = _client.getInputStream().read(buffer)) > -1) {
                data = new byte[byteCount];
                System.arraycopy(buffer, 0, data, 0, byteCount);
                dataText = new String(data, "UTF-8"); // assumption that client sends data UTF-8 encoded
                System.out.println("Message from shim: " + dataText);
                // TODO handle data
            }
            _socket.close();
            System.out.println("Shim server thread ended, spawning new...");
            _thread = new Thread(this);
            _thread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}