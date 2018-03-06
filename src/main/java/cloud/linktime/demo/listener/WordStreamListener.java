package cloud.linktime.demo.listener;

import cloud.linktime.demo.kafka.WordStreamRunnable;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Created by tim on 2018/2/23.
 */
public class WordStreamListener implements ServletContextListener {

    public void contextDestroyed(ServletContextEvent e) {
        System.out.println("WordStreamListener stop...");
    }

    public void contextInitialized(ServletContextEvent e) {
        System.out.println("WordStreamListener initialized start...");
        new Thread(new WordStreamRunnable()).start();
        System.out.println("WordStreamListener initialized end...");
    }
}
