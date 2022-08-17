import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;

public class ParserHttpHandler implements HttpHandler {
    @Override    
    public void handle(HttpExchange httpExchange) throws IOException {
        String response = httpExchange.getResponseBody().toString();



        httpExchange.sendResponseHeaders(200, response.getBytes().length);
        OutputStream os = httpExchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
}