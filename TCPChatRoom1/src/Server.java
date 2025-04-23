import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Runnable interface allows to class to be passed to a thread/thread pool
// Threads allows a program to do multiple things at the same time (can run multiple classes concurrently)
public class Server implements Runnable
{
    // server will listen to incoming connections
    // opens a new connection hanger for each client that connects

    private ArrayList<ConnectionHandler> connections;
    private ServerSocket server;
    private boolean done;
    private ExecutorService pool;

    public Server()
    {
        connections = new ArrayList<>();
        done = false;
    }

    @Override
    public void run()
    {
        try {
            /* Server socket: essentially something that listens for incoming client connections
            & allows clients to connect to the server;
            upon connection, it can exchange data with the client */
            server = new ServerSocket(9999);
            pool = Executors.newCachedThreadPool();
            while (!done) {
                // Client socket is created when its connection with server is accepted
                Socket client = server.accept();

                ConnectionHandler handler = new ConnectionHandler(client);
                connections.add(handler);
                pool.execute(handler);
            }
        } catch (Exception e) {
            shutdown();
        }
    }

    public void broadcast(String message)
    {
        for(ConnectionHandler ch : connections)
        {
            if(ch != null)
            {
                ch.sendMessage(message);
            }
        }
    }

    public void shutdown()
    {
        try {
            done = true;
            pool.shutdown();
            if (!server.isClosed()) {
                server.close();
            }
            for(ConnectionHandler ch : connections)
            {
                ch.shutdown();
            }
        }
        catch (IOException e)
        {
            // ignore
        }
    }

    class ConnectionHandler implements Runnable
    {
        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String nickname;

        public ConnectionHandler(Socket client)
        {
            this.client = client;
        }

        @Override
        public void run()
        {
            try
            {
                out = new PrintWriter(client.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                out.println("Please enter a nickname: ");
                nickname = in.readLine();
                System.out.println(nickname + " connected.");
                broadcast(nickname + " joined the chat.");
                String message;
                while((message = in.readLine()) != null)
                {
                    if(message.startsWith("/nick ")) {
                        String[] messageSplit = message.split(" ", 2);
                        if(messageSplit.length != 2)
                        {
                            out.println("No nickname provided, so no change is done.");
                        }
                        else {
                            broadcast(nickname + "renamed themselves to " + messageSplit[1]);
                            System.out.println(nickname + "renamed themselves to " + messageSplit[1]);
                            nickname = messageSplit[1];
                            out.println("Successfully changed nickname to " + nickname);
                        }
                    }
                    else if (message.startsWith("/quit")) {
                        broadcast(nickname + " left.");
                        shutdown();
                    }
                    else {
                        broadcast(nickname + ": " + message);
                    }
                }
            }
            catch (IOException e)
            {
                shutdown();
            }
        }

        public void sendMessage(String message)
        {
            out.println(message);
        }

        public void shutdown()
        {
            try {
                in.close();
                out.close();
                if (!client.isClosed()) {
                    client.close();
                }
            } catch (IOException e) {
                // ignore
            }
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.run();
    }
}
