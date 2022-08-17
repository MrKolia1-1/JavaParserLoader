import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.querz.nbt.io.SNBTUtil;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;
import net.querz.nbt.tag.StringTag;

import java.io.IOException;
import java.io.OutputStream;

public class ParserHttpHandler implements HttpHandler {
    @Override    
    public void handle(HttpExchange httpExchange) throws IOException {
        CompoundTag c = new CompoundTag();
        c.putByte("blah", (byte) 5);
        c.putString("foo", "bor");
        ListTag<StringTag> s = new ListTag<>(StringTag.class);
        s.addString("test");
        s.add(new StringTag("text"));
        c.put("list", s);
        String response = SNBTUtil.toSNBT(c);


        httpExchange.sendResponseHeaders(200, response.getBytes().length);
        OutputStream os = httpExchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
}