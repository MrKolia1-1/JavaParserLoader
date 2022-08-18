import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dev.dewy.nbt.Nbt;
import dev.dewy.nbt.api.Tag;
import dev.dewy.nbt.tags.collection.CompoundTag;
import dev.dewy.nbt.tags.collection.ListTag;
import dev.dewy.nbt.tags.primitive.IntTag;
import dev.dewy.nbt.tags.primitive.StringTag;
import netscape.javascript.JSObject;
import org.json.simple.JSONObject;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ParserHttpHandler implements HttpHandler {
    public static Map<String, String> parseQueryString(String qs) {
        Map<String, String> result = new HashMap<>();
        if (qs == null)
            return result;

        int last = 0, next, l = qs.length();
        while (last < l) {
            next = qs.indexOf('&', last);
            if (next == -1)
                next = l;

            if (next > last) {
                int eqPos = qs.indexOf('=', last);
                if (eqPos < 0 || eqPos > next)
                    result.put(URLDecoder.decode(qs.substring(last, next), StandardCharsets.UTF_8), "");
                else
                    result.put(URLDecoder.decode(qs.substring(last, eqPos), StandardCharsets.UTF_8), URLDecoder.decode(qs.substring(eqPos + 1, next), StandardCharsets.UTF_8));
            }
            last = next + 1;
        }
        return result;
    }

    @Override    
    public void handle(HttpExchange httpExchange) throws IOException {
        if (parseQueryString(httpExchange.getRequestURI().getQuery()).get("method").equalsIgnoreCase("info")) {
            parse(httpExchange, 0, "info");

        }
        if (parseQueryString(httpExchange.getRequestURI().getQuery()).get("method").equalsIgnoreCase("download")) {
            int counter = Integer.parseInt(parseQueryString(httpExchange.getRequestURI().getQuery()).get("skip"));
            parse(httpExchange, counter, "download");
        }
    }

    public void parse(HttpExchange httpExchange, int counter, String method) throws IOException {
        Nbt NBT = new Nbt();
        CompoundTag nbt = NBT.fromFile(new File(Objects.requireNonNull(Main.class.getClassLoader().getResource("ice.nbt")).getFile()));

        List<String> commands = new ArrayList<>();
        List<String> materials = new ArrayList<>();
        List<String> materialsParams = new ArrayList<>();

        ListTag<Tag> blocks = nbt.getList("blocks");
        ListTag<Tag> palette = nbt.getList("palette");

        for (Tag value : palette) {
            CompoundTag compoundTag = (CompoundTag) value;
            StringTag name = compoundTag.getString("Name");
            CompoundTag props = compoundTag.getCompound("Properties");
            if (props != null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("[");
                props.forEach(tag -> {
                    stringBuilder.append(tag.getName()).append("=").append(tag.getValue()).append(",");
                });
                String params = removeLastChar(stringBuilder.toString());
                params = params + "]<>";
                materialsParams.add(params);
            } else {
                materialsParams.add("<>");
            }
            materials.add(name.getValue().replace("minecraft:", ""));
        }

        for (Tag block : blocks) {
            CompoundTag compoundTag = (CompoundTag) block;
            IntTag state = compoundTag.getInt("state");

            if (materials.get(state.getValue()).equalsIgnoreCase("air")) {
                continue;
            }

            ListTag<Tag> pos = compoundTag.getList("pos");
            IntTag x = (IntTag) pos.get(0);
            IntTag y = (IntTag) pos.get(1);
            IntTag z = (IntTag) pos.get(2);
            commands.add("$set " + x + " " + y + " " + z + " " + materials.get(state.getValue()) + materialsParams.get(state.getValue()));
        }

        if (counter >= commands.size()) {
            String response = "Task.Complete";
            httpExchange.sendResponseHeaders(200, response.getBytes().length);
            OutputStream os = httpExchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }

        StringBuilder stringBuilder = new StringBuilder();
        var inter = commands.iterator();

        for (int i = 0; i < counter; i++) {
            inter.next();
        }

        int limit = 0;
        while (inter.hasNext()) {
            if (limit == 500) break;
            stringBuilder.append(inter.next());
            limit++;
        }

        String response = stringBuilder.toString();
        response = removeLastChar(response);
        response = removeLastChar(response);

        System.out.println("Generated parser");
        System.out.println("Block elements:" + blocks.size());
        System.out.println("Command elements:" + commands.size());
        System.out.println("Command elements response:" + limit);
        System.out.println("Material elements:" + materials.size());
        System.out.println("Text/plain length:" + response.length());

        if (method.equalsIgnoreCase("download")) {
            httpExchange.sendResponseHeaders(200, response.getBytes().length);
            OutputStream os = httpExchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
        if (method.equalsIgnoreCase("info")) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("blocks", commands.size());
            jsonObject.put("materials", materials.size());
            jsonObject.put("parts", commands.size() / 500);
            httpExchange.sendResponseHeaders(200, jsonObject.toJSONString().length());
            OutputStream os = httpExchange.getResponseBody();
            os.write(jsonObject.toJSONString().getBytes());
            os.close();
        }

    }

    public static String removeLastChar(String s) {
        return (s == null || s.length() == 0)
                ? null
                : (s.substring(0, s.length() - 1));
    }
}