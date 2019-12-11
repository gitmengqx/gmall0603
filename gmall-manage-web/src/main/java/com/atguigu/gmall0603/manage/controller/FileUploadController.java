package com.atguigu.gmall0603.manage.controller;

import org.apache.commons.lang3.StringUtils;
import org.csource.common.MyException;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@CrossOrigin
public class FileUploadController {

    @Value("${fileServer.url}")
    private String fileUrl; // http://192.168.67.224

    @RequestMapping("fileUpload")
    public String fileUpload(MultipartFile file) throws IOException, MyException {

        String imgUrl = "";
        if (file!=null){
            String configFile  = this.getClass().getResource("/tracker.conf").getFile();
            ClientGlobal.init(configFile);
            TrackerClient trackerClient=new TrackerClient();
            TrackerServer trackerServer=trackerClient.getConnection();
            StorageClient storageClient=new StorageClient(trackerServer,null);
            // String orginalFilename="e://img//zly.jpg";
            String originalFilename = file.getOriginalFilename();
            // 获取文件后缀名
            String extName = StringUtils.substringAfterLast(originalFilename, ".");
            imgUrl = fileUrl; // imgUrl=http://192.168.67.224
            String[] upload_file = storageClient.upload_file(file.getBytes(), extName, null);
            for (int i = 0; i < upload_file.length; i++) {
                String path = upload_file[i];
//                System.out.println("s = " + s);
                imgUrl+="/"+path;
			/*
			s = group1
			s = M00/00/00/wKhD4F3PpKaAScz3AACGx2c4tJ4432.jpg
			 */
                System.out.println(imgUrl);
            }
        }

        // return "http://192.168.67.224/group1/M00/00/00/wKhD4F3PpKaAScz3AACGx2c4tJ4432.jpg";
        return imgUrl;
    }
}
