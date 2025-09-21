package com.sky.utils;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import com.aliyun.oss.*;
import com.aliyun.oss.common.auth.CredentialsProviderFactory;
import com.aliyun.oss.common.auth.EnvironmentVariableCredentialsProvider;
import com.aliyun.oss.common.comm.SignVersion;
import com.sky.properties.AliOssProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@AllArgsConstructor
@Slf4j
public class AliOssUtil {


    private AliOssProperties aliOssProperties;

    /**
     * 文件上传
     *
     * @param content
     * @param objectName
     * @return
     */
    public String upload(byte[] content, String objectName) {


        String endpoint = aliOssProperties.getEndpoint();
        String bucketName = aliOssProperties.getBucketName();
        String region = aliOssProperties.getRegion();

        // 从环境变量中获取访问凭证。运行本代码示例之前，请先配置环境变量
        EnvironmentVariableCredentialsProvider credentialsProvider;
        try {
            credentialsProvider =
                    CredentialsProviderFactory.newEnvironmentVariableCredentialsProvider();
        } catch (com.aliyuncs.exceptions.ClientException e) {
            log.error("Failed to create credentials provider: {}", e.getMessage());
            throw new RuntimeException("Failed to initialize OSS credentials", e);
        }

        // 创建 OSSClient 实例
        ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();
        // 显式声明使用 V4 签名算法
        clientBuilderConfiguration.setSignatureVersion(SignVersion.V4);
        OSS ossClient = OSSClientBuilder.create()
                .endpoint(endpoint)
                .credentialsProvider(credentialsProvider)
                .region(region)
                .build();

        // 0. 创建存储空间（Bucket）
//            ossClient.createBucket(bucketName);
//            System.out.println("1. Bucket " + bucketName + " 创建成功。");
        // 1. 上传文件

//            1.1get local date as dir layer
        String dir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
//            1.2 generate a unique filename
        String newFileName = UUID.randomUUID().toString().replace("-", "") + objectName.substring(objectName.lastIndexOf("."));
        String objectNewName = dir + "/" + newFileName;

        try {
            ossClient.putObject(bucketName, objectNewName, new ByteArrayInputStream(content));
            log.info("2. 文件 " + objectNewName + " 上传成功。");
            /*// 3. 下载文件
            OSSObject ossObject = ossClient.getObject(bucketName, objectName);
            InputStream contentStream = ossObject.getObjectContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(contentStream));
            String line;
            System.out.println("3. 下载的文件内容：");
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            contentStream.close();
            // 4. 列出文件
            System.out.println("4. 列出 Bucket 中的文件：");
            ObjectListing objectListing = ossClient.listObjects(bucketName);
            for (OSSObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                System.out.println(" - " + objectSummary.getKey() + " (大小 = " + objectSummary.getSize() + ")");
            }
            // 5. 删除文件
            ossClient.deleteObject(bucketName, objectName);
            System.out.println("5. 文件 " + objectName + " 删除成功。");
            // 6. 删除存储空间（Bucket）
            ossClient.deleteBucket(bucketName);
            System.out.println("6. Bucket " + bucketName + " 删除成功。");*/
        } catch (OSSException oe) {
            System.out.println("Caught an OSSException, which means your request made it to OSS, "
                    + "but was rejected with an error response for some reason.");
            System.out.println("Error Message:" + oe.getErrorMessage());
            System.out.println("Error Code:" + oe.getErrorCode());
            System.out.println("Request ID:" + oe.getRequestId());
            System.out.println("Host ID:" + oe.getHostId());
        } catch (ClientException ce) {
            System.out.println("Caught an ClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with OSS, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message:" + ce.getMessage());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
        return endpoint.split("//")[0] + "//" + bucketName + "." + endpoint.split("//")[1] + "/" + objectNewName;
    }
}
