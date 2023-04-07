package com.xuecheng;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@SpringBootTest(classes = BigFileTest.class)
public class BigFileTest {

    // 测试分块
    @Test
    void testChunk() throws IOException {
        // 源文件
        File sourceFile = new File("D:\\IDEACODE\\upload\\11.mp4");
        // 分块文件存储路径
        String chunkFilePath = "D:\\IDEACODE\\upload\\chunk\\";
        // 分块文件大小
        int chunkSize = 1024 * 1024 * 5;
        // 分块文件个数
        int chunkNum = (int) Math.ceil(sourceFile.length() * 1.0 / chunkSize);
        // 使用流从源文件读取数据 像分块文件中写数据
        RandomAccessFile r = new RandomAccessFile(sourceFile, "r");
        // 缓存区
        byte[] bytes = new byte[1024];
        for (int i = 0; i < chunkNum; i++) {
            File chunkFile = new File(chunkFilePath + i);
            RandomAccessFile rw = new RandomAccessFile(chunkFile, "rw");
            int length = -1;
            while ((length = r.read(bytes)) != -1) {
                rw.write(bytes, 0, length);
                if (chunkFile.length() >= chunkSize) {
                    break;
                }
            }
            rw.close();
        }
        r.close();
    }

    // 将分块进行合并
    @Test
    void nameMerge() throws Exception {
        // 源文件
        File sourceFile = new File("D:\\IDEACODE\\upload\\11.mp4");
        // 分块文件存储路径
        File chunkFolder = new File("D:\\IDEACODE\\upload\\chunk\\");
        // 合并后的文件
        File mergeFile = new File("D:\\IDEACODE\\upload\\22.mp4");
        List<File> files = Arrays.asList(Objects.requireNonNull(chunkFolder.listFiles()));
        files.sort(Comparator.comparing(File::getName));
        RandomAccessFile rw = new RandomAccessFile(mergeFile, "rw");
        byte[] bytes = new byte[1024];
        for (File file : files) {
            RandomAccessFile r = new RandomAccessFile(file, "r");
            int len = -1;
            while ((len = r.read(bytes)) != -1) {
                rw.write(bytes, 0, len);
            }
            r.close();
        }
        rw.close();
        String sourceMD5 = DigestUtils.md5Hex(Files.newInputStream(new File("D:\\IDEACODE\\upload\\11.mp4").toPath()));
        String mergeMD5 = DigestUtils.md5Hex(Files.newInputStream(new File("D:\\IDEACODE\\upload\\22.mp4").toPath()));
        if (sourceMD5.equals(mergeMD5)) {
            System.out.println("文件传输成功");
        } else {
            System.out.println("文件上传失败");
        }
    }
}
