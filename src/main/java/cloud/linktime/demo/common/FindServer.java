package cloud.linktime.demo.common;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.zookeeper.ZooKeeper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by tim on 2018/2/27.
 */
public class FindServer {

    private static Map<String,String> serverCache = new HashMap<>();

    public static String getRedisAddress() throws Exception {
        return getRedisAddress("");
    }

    public synchronized static String getRedisAddress(String kafkaAddress) throws Exception {
        if(serverCache.containsKey(Constant.SERVER_REDIS)){
            return serverCache.get(Constant.SERVER_REDIS);
        }
        String appAddress = "http://"+ Constant.MASTER_MESOS + ":8080/v2/apps/linktime-redis";
        String author = "Basic " + Base64.getEncoder().encodeToString("admin:123456".getBytes());

        System.out.println(appAddress);
        URL localURL = new URL(appAddress);
        URLConnection connection = localURL.openConnection();
        HttpURLConnection httpURLConnection = (HttpURLConnection) connection;
        httpURLConnection.setRequestMethod("GET");
        httpURLConnection.setRequestProperty("Authorization", author);
        httpURLConnection.setRequestProperty("Accept-Charset", "utf-8");
        httpURLConnection.setRequestProperty("Content-Type",
                "application/json");

        InputStream inputStream = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader reader = null;
        StringBuffer resultBuffer = new StringBuffer();
        String tempLine = null;

        if (httpURLConnection.getResponseCode() >= 300) {
            throw new Exception(
                    "HTTP Request is not success, Response code is "
                            + httpURLConnection.getResponseCode());
        }

        try {
            inputStream = httpURLConnection.getInputStream();
            inputStreamReader = new InputStreamReader(inputStream);
            reader = new BufferedReader(inputStreamReader);

            while ((tempLine = reader.readLine()) != null) {
                resultBuffer.append(tempLine);
            }

        } finally {

            if (reader != null) {
                reader.close();
            }

            if (inputStreamReader != null) {
                inputStreamReader.close();
            }

            if (inputStream != null) {
                inputStream.close();
            }

        }
        String content = resultBuffer.toString();
        JSONObject jsonObject = JSON.parseObject(content);

        String address = "";
        if(StringUtils.isBlank(kafkaAddress)){
            kafkaAddress = getKafkaBroker(Constant.MASTER_MESOS + ":2181");
        }

        if(kafkaAddress.startsWith("172.") || kafkaAddress.startsWith("196.")){
            int port = jsonObject.getJSONObject("app").getJSONArray("tasks").getJSONObject(0).getJSONArray("ports").getInteger(0);
            address = Constant.MASTER_MESOS + ":" + port;
        }else{
            address = jsonObject.getJSONObject("app").getJSONObject("container").getJSONArray("portMappings").getJSONObject(0).getJSONObject("labels").getString("VIP_0");
        }
        serverCache.put(Constant.SERVER_REDIS, address);
        return address;
    }

    public synchronized static String getKafkaBroker(String servers) throws Exception {
        if(serverCache.containsKey(Constant.SERVER_KAFKA)){
            return serverCache.get(Constant.SERVER_KAFKA);
        }
        int sessionTimeout = 30000;
        ZooKeeper zk = new ZooKeeper(servers,sessionTimeout,null);
        List<String> ids = zk.getChildren(Constant.KAFKA_BROKER, false);
        for (String id : ids) {
            String brokerInfo = new String(zk.getData(Constant.KAFKA_BROKER + "/" + id, false, null));
            JSONObject object = JSON.parseObject(brokerInfo);
            String host = object.getString(Constant.KAFKA_broker_host);
            int port = object.getInteger(Constant.KAFKA_broker_port);
            String address = host + ":" + port;
            serverCache.put(Constant.SERVER_KAFKA, address);
            return address;
        }
        throw new RuntimeException("can't find kafka broker address!");

    }

    public static void main(String[] args) throws Exception {
        System.out.println(FindServer.getRedisAddress());
    }

}
