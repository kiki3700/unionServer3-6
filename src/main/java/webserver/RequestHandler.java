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
    static boolean logedin;
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
        	//get url 
        	String info = new String();
        	if(tokens[0].equals("GET")) {
        		url = tokens[1];
        		log.info("GET url : "+url);
        	}
        	if(tokens[0].equals("POST")) {
        		url = tokens[1];
        		log.info("POST url : "+url);
        		int contentLen =0;
        		//http header analy
	        	while(!"".equals(line)) {
	          		line=bf.readLine();
	          		if(line.contains("Content-Length")) {
	          			String tmp = line.split(" ")[1];
	          			contentLen = Integer.parseInt(tmp);
	          			log.info("cont-len : "+contentLen);
	          			}
	          		if(line.contains("Cookie")) {
	          			String[] cookies =line.split("; ");
	          			String cookie = new String();
	          			for(int i = 0; i < cookie.length();i++) {
	          				if(cookies[i].contains("login")) {
	          					cookie = cookies[i];
	          				}
	          			}
	          			log.debug("cookie : "+ cookie);
	          			Map logined = HttpRequestUtils.parseCookies(cookie);
	          			}
	          		}
            	info=ioUtils.readData(bf, contentLen);
        	}
        	
        	
        	//analysis request
        	//login
        	if(tokens[0].equals("POST")&&tokens[1].contains("login")) {
            	Map<String, String> map=httputils.parseQueryString(info);
            	String loginId = map.get("userId");
            	String passWord = map.get("password");
            	User user = db.findUserById(loginId);
            	log.info(loginId+ "is trying to login");
            	if(user.getPassword().equals(passWord)) {
            		loginFlag = true;
            		log.info("login success");
            	}
        	}
        	//signup
        	if(tokens[1].contains("create")) {
            	Map<String, String> map=httputils.parseQueryString(info);
            	User user = new User(map.get("userId"),map.get("password"),map.get("name"),map.get("email"));
            	log.info(map.get("userId")+" is trying to sign up.");
            	db.addUser(user);
        	}

        	
            DataOutputStream dos = new DataOutputStream(out);
            byte[] body = "Hello World".getBytes();
//            if(tokens[0].equals("GET")) { 
//            	log.info("GET : to "+url);
//	        	body =Files.readAllBytes(new File("./webapp"+url).toPath());
//	            response200Header(dos, body.length, url);
//	            responseBody(dos, body);
//            }else 
            	
        	if(tokens[0].equals("POST") && tokens[1].contains("create")) {
            	log.info("post : to index.html");
            	body =Files.readAllBytes(new File("./webapp"+"/index.html").toPath());
            	response302Header(dos, body.length, "/index.html");
            	responseBody(dos, body);
            }else if(tokens[0].equals("POST")&&tokens[1].contains("login")) {
            	log.info("post : to index.html");
            	body =Files.readAllBytes(new File("./webapp"+"/index.html").toPath());
            	response302Header(dos, body.length, "/index.html", loginFlag );
            	responseBody(dos, body);
            }else if(tokens[1].contains("user/list")) {
            	if(logedin) {
            		body =Files.readAllBytes(new File("./webapp"+url).toPath());
            	}else {
            		body =Files.readAllBytes(new File("./webapp/user/login.html").toPath());
            	}
        		response200Header(dos, body.length, url);
        		responseBody(dos, body);
            }else if(tokens[1].contains("css")) {
            	body =Files.readAllBytes(new File("./webapp"+url).toPath());
        		responseCss200Header(dos, body.length);
        		responseBody(dos, body);
            }else {
            	body =Files.readAllBytes(new File("./webapp"+url).toPath());
        		response200Header(dos, body.length, url);
        		responseBody(dos, body);
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }

    }
    
    private void response302Header(DataOutputStream dos, int lengthOfBodyContent, String location) {
        try {
            dos.writeBytes("HTTP/1.1 302 OK \r\n");
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("Location: " + location + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
    
    private void response302Header(DataOutputStream dos, int lengthOfBodyContent, String location, boolean loginFlag) {
        try {
            dos.writeBytes("HTTP/1.1 302 OK \r\n");
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("Location: " + location + "\r\n");
            log.info("login : "+ loginFlag+ " cookie set==>");
            if(loginFlag) dos.writeBytes("Set-Cookie: logined=true \r\n");
            if(!loginFlag) dos.writeBytes("Set-Cookie: logined=flase \r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
    
    private void response200Header(DataOutputStream dos, int lengthOfBodyContent, String url) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            if(!url.contains("css")) dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            if(url.contains("css")) dos.writeBytes("Content-Type: text/css;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
    private void responseCss200Header(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/css;charset=utf-8\r\n");
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
