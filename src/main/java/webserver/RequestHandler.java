package webserver;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import db.DataBase;
import model.User;
import util.HttpRequestUtils;
import util.IOUtils;

public class RequestHandler extends Thread {
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);
    static DataBase db = new DataBase();
    private Socket connection;
    static boolean loginFlag;
    IOUtils ioUtils;
    HttpRequestUtils httputils;
    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public void run() {
    	HttpRequestUtils util = new HttpRequestUtils();
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
                connection.getPort());

        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
            // TODO 사용자 요청에 대한 처리는 이 곳에 구현하면 된다.
        		//인풋 스트림 해석
        	BufferedReader bf = new BufferedReader(new InputStreamReader(in));
        	String line=bf.readLine();
        	
        	// 예외 처리
        	if(line == null) return;
        	//요청 분석
        	String[] tokens = line.split(" ");
        	String url = new String();
        	log.info("url : "+url);
        	if(tokens[0].equals("GET"))  url = tokens[1];
//        	if(tokens[1].contains("create?")) {
//        		int index =url.indexOf("?");
//        		String memberInfo = url .substring(index+1);
//        		url = url.substring(0,index);
//        		Map<String, String> map =util.parseQueryString(memberInfo);
//        		User user = new User(map.get("userId"),map.get("password"),map.get("name"),map.get("email"));
//        	}
        	//포스트 방식 회원 가입
        	int contentLen =0;
        	if(tokens[0].equals("POST")) url = tokens[1];
        	if(tokens[1].contains("login")) {
            	while(!"".equals(line)) {
              		line=bf.readLine();
              		if(line.contains("Content-Length")) {
              			String tmp = line.split(" ")[1];
              			contentLen = Integer.parseInt(tmp);
              			}
              		}
            	String info=ioUtils.readData(bf, contentLen);
            	Map<String, String> map=httputils.parseQueryString(info);
            	String loginId = map.get("userId");
            	String passWord = map.get("password");
            	User user = db.findUserById(loginId);
            	if(user.getPassword().equals(passWord)) {
            		
            	}
            	
        	}
        	
        	if(tokens[1].contains("create")) {
            	while(!"".equals(line)) {
              		line=bf.readLine();
              		if(line.contains("Content-Length")) {
              			String tmp = line.split(" ")[1];
              			contentLen = Integer.parseInt(tmp);
              			}
              		}
            	String info=ioUtils.readData(bf, contentLen);
            	Map<String, String> map=httputils.parseQueryString(info);
            	User user = new User(map.get("userId"),map.get("password"),map.get("name"),map.get("email")); 
            	db.addUser(user);
        	}

        	
            DataOutputStream dos = new DataOutputStream(out);
            byte[] body = "Hello World".getBytes();
            if(tokens[0].equals("GET")) { 
	        	body =Files.readAllBytes(new File("./webapp"+url).toPath());
	            response200Header(dos, body.length);
	            responseBody(dos, body);
            }
            if(tokens[0].equals("POST")) {
            	body =Files.readAllBytes(new File("./webapp"+"/index.html").toPath());
            	response302Header(dos, body.length, "/index.html");
            	responseBody(dos, body);
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }

    }
    
    private void response302Header(DataOutputStream dos, int lengthOfBodyContent, String location) {
        try {
            dos.writeBytes("HTTP/1.1 300 OK \r\n");
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("Location: " + location + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
    
    private void response200Header(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
