package com.crawl.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by CUI on 2016/8/26.
 * Student
 */
public class IOTools {
    public static void main(String[] args) throws IOException {
        String string="woshizhongguoren";
        File file=new File("src/resources/user1.txt");
        writeToDisk(string,file,true);
    }

    private static void writeToDisk(String string,File file,boolean isAppend) throws IOException {
        FileWriter fileWriter=new FileWriter(file,isAppend);
        fileWriter.write(string+"\n");
        fileWriter.flush();
        fileWriter.close();
    }
}
