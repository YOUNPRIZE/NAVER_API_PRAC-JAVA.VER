package com.test.controller;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

@Controller
public class HomeController {
	
	// "\n" 왜 안없어지는지? , 요약된 내용에 기사 제목, 검색한 키워드까지 추가할 것.
	
	// CLOVA Summary API를 통해 요약된 문자열을 담을 변수 생성
	static String mainContent = "";
	
	// 로그 설정
	private static final Logger logger = LoggerFactory.getLogger(HomeController.class);
	
	// Root Page 설정 -> homepage.jsp return
	@RequestMapping(value = "/", method = RequestMethod.GET)
	public String home(Locale locale, Model model) {
		logger.info("Welcome home! The client locale is {}.", locale);
		
		return "homepage";
	}
	
	// Root Page에서 입력받은 keyword 값을 Search API와 Summary API를 통해 해당 keyword에 대한 기사의 요약된 내용을 search.jsp에 출력
	@RequestMapping("/search/news")
	public static String requestParam(HttpServletRequest req, HttpServletResponse res, Model model) {
		// 입력받은 keyword를 String 변수로 생성
		String keyWord = req.getParameter("name");
		
		// 해당 문자열을 UTF-8 형태로 encoding
		// 예외처리 안해도 될듯??
		try {
			keyWord = new String(keyWord.getBytes("ISO-8859-1"), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// keyword를 Search API를 통해 기사의 URL Link를 추출해서 newUrl이라는 변수로 생성
		StringBuilder newUrl = search(keyWord);
		
		// newUrl의 Link에서 Jsoup을 통해 기사의 본문 내용을 추출 후 변수 생성
		try {
			String content = getContent(newUrl.toString()).toString();
			try {
				// 기사의 본문 내용을 Summary API를 통해 요약된 내용을 추출 후 미리 선언된 전역변수에 삽입
				mainContent = summary(content);
			
			// 본문 내용이 2,000자를 초과하거나 요청이 실패할 경우 예외처리
			} catch (IOException e) {
				return "fail";
			}
		// 검색어가 없거나 잘못된 keyword일 경우 예외처리
		} catch (IllegalArgumentException e) {
			return "null";
		}
		
		// 요약된 내용 디버깅 
		//System.out.println(mainContent);
		
		// 요약된 내용을 jsp에서 사용하기 위해 View로 변수를 전달하는 과정
		model.addAttribute("summary", mainContent);
		
		// search.jsp return
		return "search";
	}
	
	// Summary API를 통해 기사 본문을 요약된 내용으로 출력해주는 method 생성
	public static String summary(String content) throws IOException {
		
		// 1. HttpURLConnection은 http 통신을 수행할 객체입니다
		// 2. URL 객체로 connection을 만듭니다
		// 3. 응답받은 결과를 InputStream으로 받아서 버퍼에 순차적으로 쌓습니다
		
		// API에 전송할 Body JSON 형태로 생성
		JSONObject body = new JSONObject();
		JSONObject document = new JSONObject();
		JSONObject option = new JSONObject();
		
		document.put("content", content);
		option.put("language", "ko");
		option.put("model", "news");
		body.put("document", document);
		body.put("option", option);
		String newBody = body.toString();
		
		// http 요청 시 필요한 url 주소를 변수 선언
		String totalUrl = "https://naveropenapi.apigw.ntruss.com/text-summary/v1/summarize";
		
		// http 통신을 하기위한 객체 선언
		URL url = null;
		HttpURLConnection conn = null;
	    
		// http 통신 요청 후 응답 받은 데이터를 담기 위한 변수
		String responseData = "";	    	   
		BufferedReader br = null;
		StringBuffer sb = null;
	    
		// 메소드 호출 결과값을 반환하기 위한 변수
		String returnData = "";
	 
		try {
			// 파라미터로 들어온 url을 사용해 connection 실시
			url = new URL(totalUrl);	
			conn = (HttpURLConnection) url.openConnection();
	        
			// http 요청에 필요한 Header 정의 실시
			conn.setRequestMethod("POST");
			conn.setRequestProperty("X-NCP-APIGW-API-KEY-ID", "9htmafudjt");
			conn.setRequestProperty("X-NCP-APIGW-API-KEY", "4IFEEEtTp4Bm1cJ9Zhr9ftvFEMaKbFU7eWN2Hfz6");
			conn.setRequestProperty("Content-Type", "application/json");
			//conn.setRequestProperty("Content-Type", "application/json; utf-8"); //post body json으로 던지기 위함
			//conn.setRequestProperty("Accept", "application/json");
			conn.setDoOutput(true); 
			
			// OutputStream을 사용해서 post body 데이터 전송
			try (OutputStream os = conn.getOutputStream()){
				byte request_data[] = newBody.getBytes("utf-8");
				os.write(request_data);
				os.close();
			}
			catch(Exception e) {
				e.printStackTrace();
			}										        	            
	        
			// http 요청 실시
			conn.connect();

			// http 요청 후 응답 받은 데이터를 버퍼에 쌓는다
			br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));	
			sb = new StringBuffer();	       
			
			// StringBuffer에 응답받은 데이터 순차적으로 저장
			while ((responseData = br.readLine()) != null) {
				System.out.println(responseData);
				sb.append(responseData.substring(12, responseData.length()-2).replaceAll("\n", " ")); 
			}
	 
			// 메소드 호출 완료 시 반환하는 변수에 버퍼 데이터 삽입
			returnData = sb.toString(); 
		} catch (IOException e) {
			e.printStackTrace();
		} finally { 
			// http 요청 및 응답 완료 후 BufferedReader를 닫아줍니다
			// ??
			try {
				if (br != null) {
					br.close();	
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}	 
		return returnData;
	}
	
	// Search API를 통해 keyword에 대한 기사의 link를 출력해주는 method 생성
	// ??
	@SuppressWarnings({ "null", "unchecked" })
	public static StringBuilder search(String searchWord) { 
		
		// text 문자열 생성
		String text = null; 
		try { 
			// UTF-8로 인코딩된 URL을 text에 할당
			text = URLEncoder.encode(searchWord, "UTF-8"); 
		} catch (UnsupportedEncodingException e) { 
			throw new RuntimeException("검색어 인코딩 실패",e); 
		} 
		
		// 전송할 api의 URL에 담기
		String apiURL = "https://openapi.naver.com/v1/search/news?query=" + text + "&display=20";
		
		// API를 사용할 때 필요한 id, password 선언
		Map<String, String> requestHeaders = new HashMap<>(); 
		requestHeaders.put("X-Naver-Client-Id", "_CKgaKaodp7xXHSjRhjl"); 
		requestHeaders.put("X-Naver-Client-Secret", "oQrAp8M3fd"); 
		
		// API가 제공한 정보를 responseBody라는 문자열에 담기
		String responseBody = getInfo(apiURL,requestHeaders); 
		
		// JSON 문자열 -> Map 변환하는 라이브러리
		// Gson 객체 생성
		Gson gson = new Gson();
		
		// Json 문자열 -> Map
		Map<String, Object> map = gson.fromJson(responseBody, Map.class);
		
		
		String items = "items";
		
		// responseBody에서 추출한 url을 담을 변수 생성
		StringBuilder result = new StringBuilder();
		
		// Map 출력
		for (Map.Entry<String, Object> entry : map.entrySet()) {
			// Key 값이 "items" 일 때만
			if (entry.getKey().equals(items) == true) {
				
				// Key 값이 "items"인 Value값을 담을 ArrayList 변수 생성
				ArrayList<Object> list = new ArrayList<Object>();
				list = (ArrayList<Object>) entry.getValue();
				
				// link 추출
				for (int i = 0; i < list.size(); i++) {
					String str = list.get(i).toString();
					Pattern pattern = Pattern.compile("(\\blink=\\b)(.*?)(\\b, description\\b)");

					Matcher matcher = pattern.matcher(str);
							
					if (matcher.find()){
						// System.out.println(matcher.group(2).trim()); // 특정 단어 사이의 값 추출
						// naver 뉴스만 추출
						if (matcher.group(2).trim().contains("news.naver") == true) {
							// url을 변수에 담기
							result.append(matcher.group(2).trim());
							
							// keyword에 대한 link 출력
							System.out.println(result);
							break;
						}
					}
				}
			}
		}
		
		return result;
	}
	
	// URL(대부분 HTTP 프로토콜 사용)을 통해 서버와 통신하는 Java 프로그램을 개발하기 위해 URLConnection 및 HttpURLConnection 클래스를 사용
	// 예를 들어, 파일, 웹 페이지를 업로드 및 다운로드, HTTP 요청 및 응답 전송 및 검색 등을 위한 코드를 작성할 수 있다.
	private static String getInfo(String apiUrl, Map<String, String> requestHeaders) { 
		
		//URLConnection 객체 얻기
		HttpURLConnection con = connect(apiUrl); 
		try { 
			con.setRequestMethod("GET"); 
			for(Map.Entry<String, String> header :requestHeaders.entrySet()) { 
				con.setRequestProperty(header.getKey(), header.getValue()); 
			} 
				int responseCode = con.getResponseCode(); 
				if (responseCode == HttpURLConnection.HTTP_OK) {
					return readBody(con.getInputStream()); 
				} else {
					return readBody(con.getErrorStream()); 
				}
			} catch (IOException e) { 
				throw new RuntimeException("API 요청과 응답 실패", e);
			} finally { 
				con.disconnect(); 
			} 
		}

	// POST 방식으로 요청을 보내고 그 결과가 response 코드가 200(정상)인지 체크해서 정상 반환해주는 메소드
	private static HttpURLConnection connect(String apiUrl){ 
		try { 
			//URL 객체 생성
			URL url = new URL(apiUrl);
			return (HttpURLConnection)url.openConnection();
		} catch (MalformedURLException e) { 
			throw new RuntimeException("API URL이 잘못되었습니다. : " + apiUrl, e);
		} catch (IOException e) { 
			throw new RuntimeException("연결이 실패했습니다. : " + apiUrl, e);
		} 
	}
	
	// get 메서드에서 받은 결과값을 리턴해주는 메서드
	// 받은 값을 UTF-8로 읽어서 BufferedReader로 값들을 읽어서 리턴
	private static String readBody(InputStream body){ 
		InputStreamReader streamReader = new InputStreamReader(body); 
		
		try (BufferedReader lineReader = new BufferedReader(streamReader)) { 
			StringBuilder responseBody = new StringBuilder(); 
			
			String line; 
			while ((line = lineReader.readLine()) != null) { 
				responseBody.append(line); 
			} 
				
			//return responseBody.toMap();
			return responseBody.toString();
		} catch (IOException e) { 
			throw new RuntimeException("API 응답을 읽는데 실패했습니다.", e); 
		} 
	}
	
	// Jsoup을 사용해 본문을 추출하는 method
    public static StringBuilder getContent(String url) throws IllegalArgumentException {
        
    	// 본문을 담을 변수 생성
    	StringBuilder mainContent = new StringBuilder();
    	
    	try {
        	// jsoup에서 제공하는 Document 객체를 활용하여 웹페이지에서 불러온 페이지 정보를 control
            Document doc = Jsoup.connect(url).get();

            // id가 dic_area인 부분을 파싱하는 객체로 예상됨
            // javascript로 만들었을 때처럼 id가 dic_area 뿐만 아니라 다른 id일 때도 추출할 수 있는지?
            Elements elements = doc.select("#dic_area");
            
            // ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
            // 여기서 마침표를 기준으로 split해서 본문을 추출했기 때문에 해당 추출된 내용에 마침표가 아예 없었는데,
            // CLOVA Summary API에서 요청 parameter로 본문 내용을 받을 때 마침표를 기준으로 요약을 하기 때문에 (?)
            // 이 부분에서 계속 Bad Request인 상황이 발생하였다.
            // 따라서 밑에 mainContent에 append 할 때 마침표를 같이 append 해주니 Bad Request 없이 잘 Response 되었다.
            // ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
        	String[] p = elements.get(0).text().split("\\.");
        	
        	for (int i = 0; i < p.length; i++) {
                //System.out.println(p[i]);
                mainContent.append(p[i] + ".");
            }
            

        } catch (IOException e) {
            e.printStackTrace();
        }
        return mainContent;
    }
}
