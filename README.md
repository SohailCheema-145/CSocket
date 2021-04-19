# CSocket
Here's an easy solution for 2 way communication between android devices.

    **Gradle Dependency**
         
         implementation 'com.github.SohailCheema-145:CSocket:1.0'
    
    **Maven Dependency**
    
         maven { url 'https://jitpack.io' }

   **Client**
    On client side create a client socket as follows. Remember to change ip address, port and identifier accordingly.
    
    import androidx.appcompat.app.AppCompatActivity;
    import android.os.Bundle;
    import android.widget.Toast;
    import com.cheema.csocket.CInterface;
    import com.cheema.csocket.CSocket;
    import java.io.PrintWriter;
    public class MainActivity extends AppCompatActivity implements CInterface {

    private CSocket cSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        cSocket = new CSocket(this, this, "client1");
        cSocket.setServerConfig("10.10.3.99", 9000);
        cSocket.sendData("Hi from client1");
    }
    @Override
    public void onDataSent(String data) {
    }
    @Override
    public void onDataReceived(String data, PrintWriter printWriter) {
        runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, data, Toast.LENGTH_SHORT).show();
                    }});
    }
    @Override
    public void onFailure(String error) {
        runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show(); }});
    }
}


   **Server**
    On Server side create a client socket on specific port (range from 1024 - 65535) and start a server as follows.
    
    import androidx.appcompat.app.AppCompatActivity;
    import android.os.Bundle;
    import android.widget.Toast;
    import com.cheema.csocket.CInterface;
    import com.cheema.csocket.CSocket;
    import java.io.PrintWriter;
    public class MainActivity extends AppCompatActivity implements CInterface {

    private CSocket cSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        cSocket = new CSocket(this, this, "server");
        cSocket.startServer(9000);
        cSocket.sendDataToClient("Hi! i am server", "client1");
    }
    @Override
    public void onDataSent(String data) {
    }
    @Override
    public void onDataReceived(String data, PrintWriter printWriter) {
        runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, data, Toast.LENGTH_SHORT).show();
                    }});
    }
    @Override
    public void onFailure(String error) {
        runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show(); }});
    }
}

    
