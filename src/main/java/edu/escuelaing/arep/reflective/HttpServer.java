package edu.escuelaing.arep.reflective;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;

public class HttpServer {
    public static void main(String[] args) throws IOException, URISyntaxException {

        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(36000);
        } catch (IOException e) {
            System.err.println("Could not listen on port: 36000.");
            System.exit(1);
        }

        Socket clientSocket = null;
        boolean running = true;
        while (running) {
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
            String outputLine = "HTTP/1.1 200 OK\r\n"
                    + "Content-Type: application/json\r\n";
            while ((inputLine = in.readLine()) != null) {
                System.out.println("Recibí: " + inputLine);
                URI uri = new URI(inputLine.split(" ")[1]);
                String path = uri.getPath();
                String query = uri.getQuery().replace("comando=", ""); 
                if (path.startsWith("/compreflex")) {
                    String[] commandParams = query.split("\\(");
                    String command = commandParams[0];
                    String[] classAndMethod = commandParams[1].split(",");
                    String className = classAndMethod[0];
                    String methodName = classAndMethod[1];
                    if (command.equals("class")) {
                        outputLine += getClassInfo(className).toString();
                    } else if (command.equals("invoke")) {
                        outputLine += invokeMethod(className, methodName);
                    } else if (command.equals("binaryInvoke")) {
                        String[] params = getParams(command, commandParams[1]);
                        String types = getTypes(command, commandParams[1]);
                        outputLine += binaryInvokeMethod(className, methodName, types, params);
                    } else if (command.equals("unaryInvoke")) {
                        String[] params = getParams(command, commandParams[1]);
                        String type = getTypes(command, commandParams[1]);
                        outputLine += unaryInvoke(className, methodName, type, params);
                    }
                } else {
                    outputLine = "HTTP/1.1 404 Not Found\r\n";
                }
                if (!in.ready()) {
                    break;
                }
            }
            out.println(outputLine);
            out.close();
            in.close();
        }
        serverSocket.close();
    }

    private static String getTypes(String command, String params) {
        String paramsNew = params.replace(")", "");
        String[] array = paramsNew.split(",");
        String type = "";
        if (command.equals("binaryInvoke")) {
            type = array[2];
        } else if (command.equals("unaryInvoke")) {
            type = array[2];
        }
        return type;

    }

    private static String[] getParams(String command, String params) {
        String paramsNew = params.replace(")", "");
        String[] array = paramsNew.split(",");
        if (command.equals("binaryInvoke")) {
            String[] arrayNew = new String[2];
            arrayNew[0] = array[3];
            arrayNew[1] = array[5];
            return arrayNew;
        } else if (command.equals("unaryInvoke")) {
            String[] arrayNew = new String[1];
            arrayNew[0] = array[3];
            return arrayNew;
        }
        return null;

    }

    /**
     * Obteniene los campos y metodos declarados en una clase.
     * 
     * @param className Nombre de la clase.
     */
    public static String[] getClassInfo(String className) {
        try {
            Class<?> c = Class.forName(className);
            Field[] fields = c.getFields();
            Method[] methods = c.getMethods();
            String[] info = new String[fields.length + methods.length];
            for (int i = 0; i < fields.length; i++) {
                info[i] = fields[i].getName();
            }
            for (int i = 0; i < methods.length; i++) {
                info[i + fields.length] = methods[i].getName();
            }
            return info;
        } catch (ClassNotFoundException e) {
            System.out.println("Class not found");
        }
        return null;
    }

    /**
     * Invoca un metodo de una clase.
     * 
     * @param className  Nombre de la clase.
     * @param methodName Nombre del metodo.
     * @param params     Parametros del metodo.
     */
    public static String invokeMethod(String className, String methodName) {
        try {
            Class<?> c = Class.forName(className);
            Method method = c.getMethod(methodName);
            return method.invoke(null).toString();
        } catch (ClassNotFoundException e) {
            System.out.println("Class not found");
        } catch (NoSuchMethodException e) {
            System.out.println("Method not found");
        } catch (Exception e) {
            System.out.println("Error");
        }
        return null;
    }

    public static String unaryInvoke(String className, String methodName, String paramType, String[] params) {
        try {
            Class<?> c = Class.forName(className);
            String answer = "";
            if (paramType.equals("double")) {
                Method method = c.getMethod(methodName, double.class);
                answer = method.invoke(null, Double.parseDouble(params[0])).toString();
            } else if (paramType.equals("int")) {
                Method method = c.getMethod(methodName, int.class);
                answer = method.invoke(null, Integer.parseInt(params[0])).toString();
            } else if (paramType.equals("String")) {
                Method method = c.getMethod(methodName, String.class);
                answer = method.invoke(null, params[0]).toString();
            }
            return answer;
        } catch (ClassNotFoundException e) {
            System.out.println("Class not found");
        } catch (NoSuchMethodException e) {
            System.out.println("Method not found");
        } catch (Exception e) {
            System.out.println("Error");
        }
        return null;

    }

    /**
     * Invoca un metodo con dos parametros de una clase.
     * 
     * @param className  Nombre de la clase.
     * @param methodName Nombre del metodo.
     * @param params     Parametros del metodo.
     */
    public static String binaryInvokeMethod(String className, String methodName, String typeParams, String[] params) {
        try {
            Class<?> c = Class.forName(className);
            String answer = "";
            if (typeParams.equals("double")) {
                Method method = c.getMethod(methodName, double.class, double.class);
                answer = method.invoke(null, Double.parseDouble(params[0]), Double.parseDouble(params[1])).toString();
            } else if (typeParams.equals("int")) {
                Method method = c.getMethod(methodName, int.class, int.class);
                answer = method.invoke(null, Integer.parseInt(params[0]), Integer.parseInt(params[1])).toString();
            } else if (typeParams.equals("String")) {
                Method method = c.getMethod(methodName, String.class, String.class);
                answer = method.invoke(null, params[0], params[1]).toString();
            }
            return answer;
        } catch (ClassNotFoundException e) {
            System.out.println("Class not found");
        } catch (NoSuchMethodException e) {
            System.out.println("Method not found");
        } catch (Exception e) {
            System.out.println("Error");
        }
        return null;
    }

}