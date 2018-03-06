package cloud.linktime.demo.servlet;

import cloud.linktime.demo.common.Constant;
import cloud.linktime.demo.common.FindServer;
import com.alibaba.fastjson.JSON;
import redis.clients.jedis.Jedis;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.Map.Entry;

/**
 * Created by tim on 2018/2/23.
 */
public class WordServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String redisServer = null;
        try {
            redisServer = FindServer.getRedisAddress();
        } catch (Exception e) {
            e.printStackTrace();
        }
        String[] redisServers = redisServer.split(":");

        Jedis jedis = new Jedis(redisServers[0], Integer.valueOf(redisServers[1]));

        int total = 0;
        List<Map<String, Object>> sortList = new ArrayList<>();

        Map<String, String> redisContent = jedis.hgetAll(Constant.WORD_KEY);


        if(redisContent != null && redisContent.size() > 0){

            List<Entry<String, String>> result = new LinkedList<>(redisContent.entrySet());
            Collections.sort(result, new Comparator<Entry<String, String>>() {
                @Override
                public int compare(Entry<String, String> o1,
                                   Entry<String, String> o2) {
                    long i1 = Long.valueOf(o1.getValue());
                    long i2 = Long.valueOf(o2.getValue());
                    if(i1>i2){
                        return -1;
                    }else if(i1 == i2){
                        return o1.getKey().compareTo(o2.getKey());
                    }else{
                        return 1;
                    }
                }

            });

            for(Entry<String,String> entry :result){
                String word = entry.getKey();
                long count = Long.valueOf(entry.getValue());
                total += count;

                Map<String, Object> oneWord = new HashMap<>();
                oneWord.put(Constant.WORD_DETAIL_KEY, word);
                oneWord.put(Constant.WORD_DETAIL_COUNT, count);
                sortList.add(oneWord);
            }
        }

        Map dataMap = new HashMap<>();
        dataMap.put(Constant.WORD_DETAIL, sortList);
        dataMap.put(Constant.WORD_TOTAL, total);

        String result = JSON.toJSONString(dataMap);

        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET,POST");

        PrintStream out = new PrintStream(response.getOutputStream());
        out.println(result);
    }
}
