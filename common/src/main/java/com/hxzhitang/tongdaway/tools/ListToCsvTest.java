package com.hxzhitang.tongdaway.tools;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class ListToCsvTest {
    public static void listToCsv(List<Double> list, String filePath) {
        try (FileWriter writer = new FileWriter(filePath)) {
            // 遍历列表中的每个元素
            writer.append("height\n");
            for (int i = 0; i < list.size(); i++) {
                // 将元素写入 CSV 文件
                writer.append(list.get(i).toString());
                // 如果不是最后一个元素，添加逗号分隔
                if (i < list.size() - 1) {
                    writer.append("\n");
                }
            }
            // 换行
//            writer.append("\n");
            // 刷新缓冲区
            writer.flush();
        } catch (IOException e) {
            // 打印异常信息
            e.printStackTrace();
        }
    }
}

