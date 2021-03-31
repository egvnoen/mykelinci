package kelinci.socket;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import kelinci.Mem;

public class CovSend {
    public static void main(String[] args){
        try{
            ServerSocket serverSocket = new ServerSocket(60020);
            Socket socket = null;
            int count = 0;
            System.out.println("位图监控端启动");

            while (true) {
                socket = serverSocket.accept();
                InetAddress inetAddress=socket.getInetAddress();
                CovSendThread thread=new CovSendThread(socket,inetAddress);
                thread.start();
                count++;
                System.out.println("第" + count + "次测试");
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class CovSendThread extends Thread {

    Socket socket = null;
    InetAddress inetAddress=null;

    public CovSendThread(Socket socket,InetAddress inetAddress) {
        this.socket = socket;
        this.inetAddress=inetAddress;
    }

    @Override
    public void run() {

        InputStream inputStream = null;
        OutputStream outputStream = null;
        OutputStreamWriter writer = null;
        
        try {

            // When receive a message, try to send a bit map
            inputStream = socket.getInputStream();
            socket.shutdownInput();

            byte[] data = new byte[65536];
            data = Mem.getInstance().read_all();

            /*
            for(int i = 0; i < 65536; i++){
				if(data[i] != 0){
					System.out.print(i);
					System.out.print(": ");
					System.out.println(data[i]);
				}
			}
            */
            
            // clear the shm
            Mem.getInstance().write_all();
            
            // compress array: 1 byte -> 1 bit
            byte[] sendPackage = new byte[8192];
            for (int i = 0; i < 65536; i++) {
                if (data[i] > 0) {
                    int a = i / 8, b = i % 8;
                    sendPackage[a] = (byte)(sendPackage[a] | (1 << b));
                }
            }
            
            for(int i = 0; i < 8192; i++){
				if(sendPackage[i] != 0){
					System.out.print(i);
					System.out.print(": ");
					System.out.println(sendPackage[i]);
				}
			}

            outputStream = socket.getOutputStream();
            outputStream.write(sendPackage);
            outputStream.flush();
	        System.out.println("Success!");
            
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
