package com.xuecheng;

import io.minio.*;
import io.minio.errors.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SpringBootTest(classes = MinioTest.class)
public class MinioTest {
    MinioClient minioClient =
            MinioClient.builder()
                    .endpoint("http://192.168.121.135:9000/")
                    .credentials("minioadmin", "minioadmin")
                    .build();

    @Test
    void test_upload() throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {

        // 上传文件的参数信息
        UploadObjectArgs uploadObjectArgs = UploadObjectArgs.builder()
                .bucket("testbucket")
                .filename("D:\\1.mp4")
//                .object("1.mp4")
                .object("test/01/1.mp4")//对象名 放在子目录下
                .build();
        minioClient.uploadObject(uploadObjectArgs);

    }

    @Test
    void test_delete() throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {


        //RemoveObjectArgs
        RemoveObjectArgs removeObjectArgs =
                RemoveObjectArgs.builder()
                        .bucket("testbucket")
                        .object("1.mp4")
                        .build();

        //删除文件
        minioClient.removeObject(removeObjectArgs);


    }

    // 查询文件

    @Test
    void test_getFile() throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        GetObjectArgs getObjectArgs = GetObjectArgs.builder().bucket("testbucket").object("test/01/1.mp4").build();
        // 查询远程服务得到一个流对象
        FilterInputStream inputStream = minioClient.getObject(getObjectArgs);
        FileOutputStream fileOutputStream = new FileOutputStream(new File("D:\\11.mp4"));
        IOUtils.copy(inputStream, fileOutputStream);

        // 校验文件的完整性 对文件内容进行MD5加密
        FileInputStream inputStream1 = new FileInputStream(new File("D:\\1.mp4"));
        String source = DigestUtils.md5Hex(inputStream1);
        FileInputStream inputStream2 = new FileInputStream(new File("D:\\11.mp4"));
        String local = DigestUtils.md5Hex(inputStream2);
        if (source.equals(local)) {
            System.out.println("下载成功");
        } else {
            System.out.println("下载失败");
        }
    }

    // 将分块文件上传到minio
    @Test
    void uploadChunk() throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        for (int i = 0; i < 2; i++) {
            // 上传文件的参数信息
            UploadObjectArgs uploadObjectArgs = UploadObjectArgs.builder()
                    .bucket("testbucket")
                    .filename("D:\\IDEACODE\\upload\\chunk\\" + i)
                    .object("chunk/" + i)
                    .build();
            minioClient.uploadObject(uploadObjectArgs);
        }
    }

    // 合并分块文件

    @Test
    void merge() throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
//        ArrayList<ComposeSource> sources = new ArrayList<>();
//        for (int i = 0; i < 6; i++) {
//            sources.add(
//                    ComposeSource.builder()
//                            .bucket("testbucket")
//                            .object("chunk/" + i)
//                            .build());
//        }
        List<ComposeSource> sources = Stream.iterate(0, i -> ++i).limit(2).map(i ->
                ComposeSource
                        .builder()
                        .bucket("testbucket")
                        .object("chunk/" + i)
                        .build()
        ).collect(Collectors.toList());
        ComposeObjectArgs composeObjectArgs = ComposeObjectArgs.builder()
                .bucket("testbucket")
                .object("merge01.mp4")
                .sources(sources)
                .build();
        minioClient.composeObject(composeObjectArgs);

    }
}
