package edu.escuelaing.arep.reflective;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class FacadeWeb {

    private static final String USER_AGENT = "Mozilla/5.0";
    private static final String GET_URL = "http://localhost:36000/compreflex?";

    public static void main(String[] args) throws IOException, URISyntaxException {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(36001);
        } catch (IOException e) {
            System.err.println("Could not listen on port: 36001.");
            System.exit(1);
        }
        boolean running = true;
        while (running) {
            Socket clientSocket = null;
            try {
                System.out.println("Listo para recibir ...");
                clientSocket = serverSocket.accept();
            } catch (IOException e) {
                System.err.println("Accept failed.");
                System.exit(1);
            }
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String inputLine;
            String outputLine = "";
            inputLine = in.readLine();
            if (inputLine != null) {
                URI uri = new URI(inputLine.split(" ")[1]);
                String path = uri.getPath();
                String query = uri.getQuery();
                if (path.startsWith("/consulta"))
                    outputLine = serviceRest(query);
                else if (path.startsWith("/cliente"))
                    outputLine = indexClient();
                out.println(outputLine);
            }
            out.close();
            in.close();
            clientSocket.close();
        }
        serverSocket.close();
    }

    public static String indexClient() {
        return "HTTP/1.1 200 OK\r\n"
                + "Content-Type: text/html\r\n"
                + "\r\n" +
                "<!DOCTYPE html>\n" +
                "<html>\n" +
                "    <head>\n" +
                "        <title>ChatGPT</title>\n" +
                "        <meta charset=\"UTF-8\">\n" +
                "        <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    </head>\n" +
                "    <body>\n" +
                "        <h1>Chat</h1>\n" +
                "        <form action=\"/hello\">\n" +
                "            <label for=\"command\">Comando:</label><br><br>\n" +
                "            <input type=\"text\" id=\"command\" name=\"name\" value=\"invoke\"><br><br>\n" +
                "            <label for=\"class\">Clase:</label><br><br>\n" +
                "            <input type=\"text\" id=\"class\\\" name=\"name\" value=\"java.lang.System\" ><br><br>\n" +
                "            <label for=\"params\">Parametros separados por \",\":</label><br><br>\n" +
                "            <input type=\"text\" id=\"params\" name=\"name\" value=\"getenv\"><br><br>\n" +
                "            <input type=\"button\" value=\"Consultar\" onclick=\"loadGetMsg()\">\n" +
                "        </form> \n" +
                "        <h3>Informacion</h3>\n" +
                "        <div id=\"getrespmsg\"></div>\n" +
                "\n" +
                getJS() +
                "    </body>";
    }

    public static String getJS() {
        return "        <script>\n" +
                "            function loadGetMsg() {\n" +
                "                let commandVar = document.getElementById(\"command\").value;\n" +
                "                let nameClass = document.getElementById(\"class\").value;\n" +
                "                let paramsVar = document.getElementById(\"params\").value;\n" +
                "                const xhttp = new XMLHttpRequest();\n" +
                "                xhttp.onload = function() {\n" +
                "                    document.getElementById(\"getrespmsg\").innerHTML =\n" +
                "                    this.responseText;\n" +
                "                }\n" +
                "                xhttp.open(\"GET\", \"/consulta?comando=\"+commandVar +\"(\" + paramsVar +\")\");\n" +
                "                xhttp.send();\n" +
                "            }\n" +
                "        </script>\n";
    }

    /**
     * Hace la petición para ReflexCalculator para que haga el cómputo.
     *
     * @param query Comando y parámetros para hacer el cómputo.
     * @return El resultado del cómputo dado por ReflexCalculator.
     * @throws IOException Signals that an I/O exception of some sort has occurred.
     */
    public static String serviceRest(String query) throws IOException {
        String outputLine = "";
        URL obj = new URL(GET_URL + query);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", USER_AGENT);
        int responseCode = con.getResponseCode();
        System.out.println("GET Response Code :: " + responseCode);
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader inC = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLinee;
            StringBuffer response = new StringBuffer();
            while ((inputLinee = inC.readLine()) != null) {
                response.append(inputLinee);
            }
            outputLine = "HTTP/1.1 200 OK\r\n"
                    + "Content-Type: application/json\r\n"
                    + "\r\n" + response;
            inC.close();
        } else {
            System.out.println("GET request not worked");
        }
        System.out.println("GET DONE");
        return outputLine;
    }
}
