package network.io;

import java.io.DataOutputStream;
import java.net.Socket;
import java.util.ArrayList;

import network.handler.IMessageSendCollect;
import network.session.ISession;

public class Sender implements Runnable {

    private ISession session;
    private ArrayList<Message> messages;
    private DataOutputStream dos;
    private IMessageSendCollect sendCollect;

    public Sender(ISession session, Socket socket) {
        try {
            this.session = session;
            this.messages = new ArrayList<>();
            setSocket(socket);
        } catch (Exception ignored) {
        }
    }

    public Sender setSocket(Socket socket) {
        try {
            this.dos = new DataOutputStream(socket.getOutputStream());
        } catch (Exception ignored) {
        }
        return this;
    }

    @Override
    public void run() {
        while (this.session != null && this.session.isConnected()) {
            try {
                while (this.session != null && this.messages != null && !this.messages.isEmpty()) {
                    Message message = this.messages.remove(0);
                    if (message != null) {
                        doSendMessage(message);
                    }
                    message = null;
                }
                Thread.sleep(1L);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void doSendMessage(Message message) throws Exception {
        this.sendCollect.doSendMessage(this.session, this.dos, message);
    }

    public synchronized void sendMessage(Message msg) {
        if (this.session != null && this.session.isConnected()) {
            this.messages.add(msg);
        }
    }

    public void setSend(IMessageSendCollect sendCollect) {
        this.sendCollect = sendCollect;
    }

    public int getNumMessages() {
        if (this.messages != null) {
            return this.messages.size();
        }
        return -1;
    }

    public void close() {
        if (this.messages != null) {
            this.messages.clear();
        }
        if (this.dos != null) {
            try {
                this.dos.close();
            } catch (Exception ignored) {
            }
        }
    }

    public void dispose() {
        this.session = null;
        this.messages = null;
        this.sendCollect = null;
        this.dos = null;
    }
}
