package com.crawl.zhihu.task;

import com.crawl.config.Config;
import com.crawl.dao.JDBCTools;
import com.crawl.dao.ZhihuDAO;
import com.crawl.entity.Page;
import com.crawl.entity.User;
import com.crawl.parser.zhihu.ZhiHuUserFollowingListPageParser;
import com.crawl.parser.zhihu.ZhiHuUserIndexDetailPageParser;
import com.crawl.util.MyLogger;
import com.crawl.zhihu.ZhiHuHttpClient;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Administrator on 2016/8/24 0024.
 * 解析网页任务
 */
public class ParseTask implements Runnable {
    private static Logger logger = MyLogger.getLogger(ParseTask.class);
    private Page page;
    private static ZhiHuHttpClient zhiHuHttpClient = ZhiHuHttpClient.getInstance();
    /**
     * 统计用户数量
     */
    public static AtomicInteger parseUserCount = new AtomicInteger(0);
    public static volatile boolean isStopDownload = false;
    public ParseTask(Page page){
        this.page = page;
    }
    @Override
    public void run() {
        parse();
    }
    private void parse(){
        Document doc = Jsoup.parse(page.getHtml());
        if(doc.select("title").size() != 0) {
            /**
             *  包含title标签,用户主页
             */
            User u = ZhiHuUserIndexDetailPageParser.getInstance().parse(page);
            try {
                writeToDisk(u.toString(),new File("src/resources/user.txt"),true);
                logger.info("保存信息成功");
            } catch (IOException e) {
                e.printStackTrace();
            }
            parseUserCount.incrementAndGet();
            logger.info("解析用户成功:" + u.toString());
            for(int i = 0;i < u.getFollowees()/20 + 1;i++) {
                /**
                 * 当下载网页队列小于100时才获取该用户关注用户
                 * 防止下载网页线程池任务队列过量增长
                 */
                if (!isStopDownload && zhiHuHttpClient.getDownloadThreadExecutor().getQueue().size() <= 100) {
                    /**
                     * 获取关注用户列表,因为知乎每次最多返回20个关注用户
                     */
                    String userFolloweesUrl = formatUserFolloweesUrl(20*i, u.getHashId());
                    zhiHuHttpClient.getDownloadThreadExecutor().execute(new DownloadTask(userFolloweesUrl));
                }
            }
        }
        else {
            /**
             * "我关注的人"列表页
             */
            if(!isStopDownload && zhiHuHttpClient.getDownloadThreadExecutor().getQueue().size() <= 100){
                List<String> userIndexHref = ZhiHuUserFollowingListPageParser.getInstance().parse(page);
                for(String url : userIndexHref){
                    zhiHuHttpClient.getDownloadThreadExecutor().execute(new DownloadTask(url));
                }
            }
        }
    }
    private static void writeToDisk(String string, File file, boolean isAppend) throws IOException {
        FileWriter fileWriter=new FileWriter(file,isAppend);
        fileWriter.write(string+"\n");
        fileWriter.flush();
        fileWriter.close();
    }
    public String formatUserFolloweesUrl(int offset, String userHashId){
        String url = "https://www.zhihu.com/node/ProfileFolloweesListV2?params={%22offset%22:" + offset + ",%22order_by%22:%22created%22,%22hash_id%22:%22" + userHashId + "%22}";
        url = url.replaceAll("[{]", "%7B").replaceAll("[}]", "%7D").replaceAll(" ", "%20");
        return url;
    }
}
