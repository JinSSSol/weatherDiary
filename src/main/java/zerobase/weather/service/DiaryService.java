package zerobase.weather.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import zerobase.weather.domain.Diary;
import zerobase.weather.repository.DiaryRepository;

@Service
public class DiaryService {

	@Value("${openweathermap.key}")
	private String apiKey;

	private final DiaryRepository diaryRepository;

	public DiaryService(DiaryRepository diaryRepository) {
		this.diaryRepository = diaryRepository;
	}

	public void createDiary(LocalDate date, String text) {
		// Open weather map 에서 날씨 데이터 가져오기
		String weatherData = getWeatherString();

		// 받아온 날씨 json 파싱하기
		Map<String, Object> parsedWeather = parseWeather(weatherData);

		// 파싱된 데이터 + 일기 값 DB에 저장
		Diary nowDiary = new Diary();
		nowDiary.setWeather(parsedWeather.get("main").toString());
		nowDiary.setIcon(parsedWeather.get("icon").toString());
		nowDiary.setTemperature((Double)parsedWeather.get("temp"));
		nowDiary.setText(text);
		nowDiary.setDate(date);

		diaryRepository.save(nowDiary);
	}

	private String getWeatherString() {
		String apiUrl = "https://api.openweathermap.org/data/2.5/weather?lat=44.34&lon=10.99&appid=" + apiKey;

		try {
			// url 생성
			URL url = new URL(apiUrl);

			// url GET 요청 보내기
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");

			// 응답 메시지 받고, br로 받기
			int responseCode = connection.getResponseCode();
			BufferedReader br;

			if (responseCode == 200) {
				br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			} else {
				br = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
			}

			// br -> StringBuilder -> String
			String inputLine;
			StringBuilder response = new StringBuilder();
			while ((inputLine = br.readLine()) != null) {
				response.append(inputLine);
			}
			br.close();
			return response.toString();
		} catch (Exception e) {
			System.out.println(e.toString());
		}
		return "";
	}

	private Map<String, Object> parseWeather(String jsonString) {
		JSONParser jsonParser = new JSONParser();
		JSONObject jsonObject;

		try {
			jsonObject = (JSONObject) jsonParser.parse(jsonString);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
		Map<String, Object> resultMap = new HashMap<>();

		JSONObject mainData = (JSONObject) jsonObject.get("main");
		resultMap.put("temp", mainData.get("temp"));
		JSONArray weatherArray = (JSONArray) jsonObject.get("weather");
		JSONObject weatherData = (JSONObject) weatherArray.get(0);
		resultMap.put("main", weatherData.get("main"));
		resultMap.put("icon", weatherData.get("icon"));
		return resultMap;
	}


	public List<Diary> readDiary(LocalDate date) {
		return diaryRepository.findAllByDate(date);
	}

	public List<Diary> readDiaries(LocalDate startDate, LocalDate endDate) {
		return diaryRepository.findAllByDateBetween(startDate, endDate);
	}

	public void updateDiary(LocalDate date, String text) {
		Diary nowDiary = diaryRepository.getFirstByDate(date);
		nowDiary.setText(text);
		diaryRepository.save(nowDiary);
	}

	public void deleteDiary(LocalDate date) {
		diaryRepository.deleteAllByDate(date);
	}
}
