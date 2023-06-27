package com.mscloud.files.ftp.util;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;


/**
 * FTP上传
 * @author User
 *
 */
@Slf4j
@Component
public class FtpClientUtil {

	private  FTPClient ftpClient =null;

	@Value("${mscloud.ftp.ip:'127.0.0.1'}")
	private String ip;
	@Value("${mscloud.ftp.port:'21'}")
	private int port;
	@Value("${mscloud.ftp.username:'username'}")
	private String username;
	@Value("${mscloud.ftp.password:'password'}")
	private String password;
	@Value("${mscloud.ftp.workDir:''}")
	private String workDir;


	public FtpClientUtil(){
		ftpClient = new FTPClient();
	}

	/**
	 * 连接FTP
	 */
	public FTPClient open() throws IOException {


		 //设置超时
		 ftpClient.setConnectTimeout(60*60*1000);
		 //设置编码
		 ftpClient.setControlEncoding("UTF-8");

		 //连接FTP服务器
		ftpClient.connect(ip, port);
		 //登录FTP服务器
		 ftpClient.login(username, password);
		 ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
		 //是否成功登录FTP服务器
		 int replyCode = ftpClient.getReplyCode();
		 if(!FTPReply.isPositiveCompletion(replyCode)){
			return ftpClient;
		 }


		 return ftpClient;
	}
	
	/**
	 * 关闭FTP
	 */
	public void close() {
		try {
			ftpClient.logout();
		} catch (IOException e) {
			e.printStackTrace();
			
		}finally {
			if(ftpClient.isConnected()){
	 			try {
	 				ftpClient.disconnect();
	 			} catch (IOException e) {
	 				e.printStackTrace();
	 			}
	 		}
		}
	}
	public String getIp() {
		return ip;
	}

	public int getPort() {
		return port;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public String getWorkDir() {
		return workDir;
	}
}
