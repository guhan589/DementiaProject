package com.example.last;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import static android.speech.tts.TextToSpeech.ERROR;

public class TestActivity extends AppCompatActivity {


    ArrayList<Problem> problems;
    TextView exampleTextView;
    ImageView exampleImageView;
    EditText answerEditText;
    Button exampleButton;
    TextToSpeech tts;

    int problemsnum = 0;
    int year, month, day, week, score = 0;
    String state = null, city=null, town=null;  //state: 도, 특별시, 광역시  city: 시 군  town: 면/동/읍
    String season = null, days = null, speak = null;
    String[] buffer, buffer1;
    AssetManager am;
    InputStream is = null;
    Double latitude=0.0,longitude=0.0;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        am = getResources().getAssets();
        String num = null;

        StringTokenizer tokens;
        final int PERMISSION = 1;
        problems = new ArrayList<>();
        exampleTextView = (TextView) findViewById(R.id.exampleTextView); //문제 넣을 텍스트뷰
        exampleImageView = (ImageView) findViewById(R.id.exampleImageView);
        answerEditText = (EditText) findViewById(R.id.answerEditText);
        exampleButton = (Button) findViewById(R.id.exampleButton);

        final Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,getPackageName());//음성 검색을 위한
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ko-kr");//인식할 언어 설정

        if (Build.VERSION.SDK_INT >= 23) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET, Manifest.permission.RECORD_AUDIO,Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.CALL_PHONE}, PERMISSION);
        }


        startLocationService();//내위치 경도 위도 값 추출하는 함수 호출
        String address = getAddress(latitude,longitude);//경도 위도값을 매개변수로 하여 주소를 가져옴
        tokens = new StringTokenizer(address," ");
        buffer1 = new String[tokens.countTokens()];
        int i = 0;
        while(tokens.hasMoreTokens()){
            buffer1[i] = tokens.nextToken();
            if(i==1)
                state=buffer1[i];
            if(i==2)
                city=buffer1[i];
            if(i==3)
                town=buffer1[i];
            i++;
        }

        // 캘린더 객체로  5번까지 답 생성 하기 .
        Calendar calendar = new GregorianCalendar(Locale.KOREA);
        year = calendar.get(Calendar.YEAR);
        month = calendar.get(Calendar.MONTH) + 1;
        day = calendar.get(Calendar.DAY_OF_MONTH);
        week = calendar.get(Calendar.DAY_OF_WEEK);


        if (month == 3 || month == 4 || month == 5)
            season = "봄";
        else if (month == 6 || month == 7 || month == 8)
            season = "여름";
        else if (month == 9 || month == 10 || month == 11)
            season = "가을";
        else if (month == 12 || month == 1 || month == 2)
            season = "겨울";

        switch (week) {
            case 1:
                days = "일";
                break;
            case 2:
                days = "월";
                break;
            case 3:
                days = "화";
                break;
            case 4:
                days = "수";
                break;
            case 5:
                days = "목";
                break;
            case 6:
                days = "금";
                break;
            case 7:
                days = "토";
                break;
        }



        try {
            is = am.open("test.txt");

            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String teasd= null;
            StringBuilder stringBuilder= new StringBuilder();

            while ((teasd=br.readLine()) != null) {
                stringBuilder.append(teasd);
            }


            tokens = new StringTokenizer(stringBuilder.toString(), "*");

            while (tokens.hasMoreTokens()) {
                num = tokens.nextToken();       //번호
                num = num.replaceAll("\r\n", "");//토큰으로 문자열 자를 때 문장 끝에 \r\n이 인식됨
                String example = tokens.nextToken();   // 문제
                String answer = tokens.nextToken();   // 답
                String url = tokens.nextToken();   // 성별

                Problem problem = new Problem(example, answer, url, num);
                problems.add(problem);
            }


            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (is != null) {
            try {
                is.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != ERROR) {
                    tts.setLanguage(Locale.KOREAN);

                    //tts.setPitch(0.8f);// 말하는 속도 조절  기본속도: 1.0f
                    String str = "테스트를 시작하겠습니다."+problems.get(problemsnum).example;//첫번째 문제를 String str에 저장
                    tts.speak(str, TextToSpeech.QUEUE_FLUSH, null);//str문자열 음성 출력 및 QUEUE_FLUSH: 음성출력 전 출력메모리 리셋

                }
            }
        });


        exampleTextView.setText(problems.get(problemsnum).num+"."+problems.get(problemsnum).example);//첫번째 문제의 문제 출력


        exampleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {

                    if (problemsnum < problems.size()-1) {
                        String strnum = problems.get(problemsnum).num;
                        int num1 = Integer.parseInt(strnum);
                        Log.d("TAG", "problemsnum: "+problemsnum);
                        Log.d("TAG", "problems.size(): "+problems.size());
                        Log.d("TAG", "num1: "+num1);
                        Log.d("TAG", "score1: "+score);
                        switch (num1) {
                            case 1:
                                if (!answerEditText.getText().toString().equals("")&&Integer.parseInt(answerEditText.getText().toString())==year) {
                                    score += 1; //1번문제를 맞췄을시
                                }
                                problemsnum++;
                                answerEditText.setText("");    //답맞는지 확인후 문제를 바꿔준다 .

                                exampleTextView.setText((num1+1) +"." + problems.get(problemsnum).example);
                                String str = problems.get(problemsnum).example;
                                tts.speak(str, TextToSpeech.QUEUE_FLUSH, null);//첫 매개변수: 문장   두번째 매개변수:Flush 기존의 음성 출력 끝음 Add: 기존의 음성출력을 이어서 출력
                                break;
                            case 2:
                                if(!answerEditText.getText().toString().equals("")&&(answerEditText.getText().toString().equals(season)))
                                    score+=1;
                                problemsnum++;
                                answerEditText.setText("");

                                exampleTextView.setText((num1+1) +"." + problems.get(problemsnum).example);
                                str = problems.get(problemsnum).example;
                                tts.speak(str, TextToSpeech.QUEUE_FLUSH, null);//첫 매개변수: 문장   두번째 매개변수:Flush 기존의 음성 출력 끝음 Add: 기존의 음성출력을 이어서 출력
                                break;
                            case 3:
                                if(!answerEditText.getText().toString().equals("")&&Integer.parseInt(answerEditText.getText().toString())==day)
                                    score += 1; //1번문제를 맞췄을시
                                answerEditText.setText("");
                                problemsnum++;

                                exampleTextView.setText((num1+1) +"." + problems.get(problemsnum).example);
                                str = problems.get(problemsnum).example;
                                tts.speak(str, TextToSpeech.QUEUE_FLUSH, null);//첫 매개변수: 문장   두번째 매개변수:Flush 기존의 음성 출력 끝음 Add: 기존의 음성출력을 이어서 출력
                                break;
                            case 4:
                                if(!answerEditText.getText().toString().equals("")&&(answerEditText.getText().toString().equals(days)))
                                    score += 1; //1번문제를 맞췄을시
                                answerEditText.setText("");
                                problemsnum++;

                                exampleTextView.setText((num1+1) +"." + problems.get(problemsnum).example);
                                str = problems.get(problemsnum).example;
                                tts.speak(str, TextToSpeech.QUEUE_FLUSH, null);//첫 매개변수: 문장   두번째 매개변수:Flush 기존의 음성 출력 끝음 Add: 기존의 음성출력을 이어서 출력
                                break;
                            case 5:
                                if(!answerEditText.getText().toString().equals("")&&(Integer.parseInt(answerEditText.getText().toString())==month))
                                    score += 1; //1번문제를 맞췄을시

                                answerEditText.setText("");
                                problemsnum++;

                                exampleTextView.setText((num1+1) +"." + problems.get(problemsnum).example);
                                str = problems.get(problemsnum).example;
                                tts.speak(str, TextToSpeech.QUEUE_FLUSH, null);//첫 매개변수: 문장   두번째 매개변수:Flush 기존의 음성 출력 끝음 Add: 기존의 음성출력을 이어서 출력
                                break;
                            case 6:
                            case 7:
                            case 8:
                                if(!answerEditText.getText().toString().equals("")&&(answerEditText.getText().toString().equals(state))||(answerEditText.getText().toString().equals(city))||(answerEditText.getText().toString().equals(town)))
                                    score += 1; //1번문제를 맞췄을시
                                problemsnum++;
                                answerEditText.setText("");    //답맞는지 확인후 문제를 바꿔준다 .
                                exampleTextView.setText((num1+1) +"." + problems.get(problemsnum).example);
                                str = problems.get(problemsnum).example;
                                tts.speak(str, TextToSpeech.QUEUE_FLUSH, null);//첫 매개변수: 문장   두번째 매개변수:Flush 기존의 음성 출력 끝음 Add: 기존의 음성출력을 이어서 출력
                                break;
                            case 9:
                            case 10:
                            case 11:
                            case 12:
                                if(!answerEditText.getText().toString().equals("")&&
                                        (Integer.parseInt(answerEditText.getText().toString())==Integer.parseInt(problems.get(problemsnum).answer)))
                                    score += 1; //9번 정답 맞는지 확인하기 .

                                answerEditText.setText("");
                                problemsnum++;

                                exampleTextView.setText((num1+1) +"." + problems.get(problemsnum).example);
                                str = problems.get(problemsnum).example;
                                tts.speak(str, TextToSpeech.QUEUE_FLUSH, null);//첫 매개변수: 문장   두번째 매개변수:Flush 기존의 음성 출력 끝음 Add: 기존의 음성출력을 이어서 출력
                                break;
                            case 13:
                                exampleButton.setEnabled(false);//버튼 비활성화
                                if(!answerEditText.getText().toString().equals("")&&
                                        (Integer.parseInt(answerEditText.getText().toString())==Integer.parseInt(problems.get(problemsnum).answer)))
                                    score += 1; //9번 정답 맞는지 확인하기 .

                                answerEditText.setText("");
                                problemsnum++;

                                answerEditText.setText("호출된 단어를 다 듣고 따라 말하십시오 그 후에 버튼이 활성화됩니다.ㄴ");
                                exampleTextView.setText((num1+1) +"." + problems.get(problemsnum).example);
                                str = problems.get(problemsnum).example;
                                str += "단어가 출력됩니다. 집중하세요.  " + problems.get(problemsnum).answer;
                                tts.speak(str, TextToSpeech.QUEUE_FLUSH, null);//첫 매개변수: 문장   두번째 매개변수:Flush 기존의 음성 출력 끝음 Add: 기존의 음성출력을 이어서 출력
                                new Handler().postDelayed(new Runnable() {//tts출력후 음성인식 시작
                                    @Override
                                    public void run() {
                                        Toast.makeText(getApplicationContext(), "음석인식이 활성화되었습니다. 말을 한 후에  확인버튼을 누르세요. ", Toast.LENGTH_SHORT).show();
                                        SpeechRecognizer speechRecognizer = SpeechRecognizer.createSpeechRecognizer(getApplicationContext());
                                        speechRecognizer.setRecognitionListener(listener);
                                        speechRecognizer.startListening(intent);
                                        exampleButton.setEnabled(true);//버튼활성화
                                    }
                                },18500);

                                break;
                            case 14:
                                String answer = problems.get(problemsnum).answer;
                                StringTokenizer tokens = new StringTokenizer(answer,",");
                                buffer = new String[tokens.countTokens()];
                                int i=0;
                                while(tokens.hasMoreTokens()){
                                    buffer[i] = tokens.nextToken();
                                    i++;
                                }
                                for(i = 0; i<buffer.length;i++){
                                    if(speak==null)//음성인식을 못할경우
                                        continue;
                                    else if(speak.contains(buffer[i])){//무조건 음성호출을 입력받아야하고 안받고 버튼을 누르면 오류가 생김
                                        score+=1;
                                    }
                                }

                                problemsnum++;
                                answerEditText.setText("");
                                exampleTextView.setText((num1+1) +"." + problems.get(problemsnum).example);
                                str = problems.get(problemsnum).example;

                                is = am.open(problems.get(problemsnum).url+".png");
                                Bitmap bm= BitmapFactory.decodeStream(is);
                                exampleImageView.setImageBitmap(bm);

                                tts.speak(str, TextToSpeech.QUEUE_FLUSH, null);//첫 매개변수: 문장   두번째 매개변수:Flush 기존의 음성 출력 끝음 Add: 기존의 음성출력을 이어서 출력
                                break;
                            case 15:
                                if(!answerEditText.getText().toString().equals("")&&
                                        (answerEditText.getText().toString().equals(problems.get(problemsnum).answer)))
                                    score += 1;
                                Log.d("TAG", "answer1: "+problems.get(problemsnum).answer);
                                problemsnum++;
                                answerEditText.setText("");
                                exampleTextView.setText((num1+1) +"." + problems.get(problemsnum).example);
                                str = problems.get(problemsnum).example;
                                tts.speak(str, TextToSpeech.QUEUE_FLUSH, null);//첫 매개변수: 문장   두번째 매개변수:Flush 기존의 음성 출력 끝음 Add: 기존의 음성출력을 이어서 출력

                                is = am.open(problems.get(problemsnum).url+".png");
                                bm= BitmapFactory.decodeStream(is);
                                exampleImageView.setImageBitmap(bm);
                                break;
                            case 16:
                                if(!answerEditText.getText().toString().equals("")&&
                                        (answerEditText.getText().toString().equals(problems.get(problemsnum).answer)))
                                    score+=1;
                                Log.d("TAG", "answer2: "+problems.get(problemsnum).answer);
                                Log.d("TAG", "score2: "+score);
                                problemsnum++;
                                break;
                        }
                    } else {
                        Intent loginIntent = new Intent(TestActivity.this, ResultActivity.class);
                        startActivity(loginIntent);
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        });

    }
    private RecognitionListener listener = new RecognitionListener(){

        @Override
        public void onReadyForSpeech(Bundle params) {

        }

        @Override
        public void onBeginningOfSpeech() {

        }

        @Override
        public void onRmsChanged(float rmsdB) {

        }

        @Override
        public void onBufferReceived(byte[] buffer) {

        }

        @Override
        public void onEndOfSpeech() {

        }

        @Override
        public void onError(int error) {
            String message;
            switch (error) {
                case SpeechRecognizer.ERROR_AUDIO:
                    message = "오디오 에러";
                    tts.speak(message.toString(), TextToSpeech.QUEUE_FLUSH, null);
                    break;
                case SpeechRecognizer.ERROR_CLIENT:
                    message = "클라이언트 에러";
                    tts.speak(message.toString(), TextToSpeech.QUEUE_FLUSH, null);
                    break;
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                    message = "퍼미션 없음";
                    tts.speak(message.toString(), TextToSpeech.QUEUE_FLUSH, null);
                    break;
                case SpeechRecognizer.ERROR_NETWORK:
                    message = "네트워크 에러";
                    tts.speak(message.toString(), TextToSpeech.QUEUE_FLUSH, null);
                    break;
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                    message = "네트웍 타임아웃";
                    tts.speak(message.toString(), TextToSpeech.QUEUE_FLUSH, null);
                    break;
                case SpeechRecognizer.ERROR_NO_MATCH:
                    message = "찾을 수 없음";
                    tts.speak(message.toString(), TextToSpeech.QUEUE_FLUSH, null);
                    break;
                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                    message = "음성인식 서비스가 과부하 되었습니다.";
                    tts.speak(message.toString(), TextToSpeech.QUEUE_FLUSH, null);
                    break;
                case SpeechRecognizer.ERROR_SERVER:
                    message = "서버 장애";
                    tts.speak(message.toString(), TextToSpeech.QUEUE_FLUSH, null);
                    break;
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                    message = "말하는 시간초과";
                    tts.speak(message.toString(), TextToSpeech.QUEUE_FLUSH, null);
                    break;
                default:
                    message = "알 수 없는 오류임";
                    tts.speak(message.toString(), TextToSpeech.QUEUE_FLUSH, null);
                    break;
            }
            Toast.makeText(getApplicationContext(), "에러가 발생하였습니다. : " + message, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onResults(Bundle results) {
            ArrayList<String> list = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);//사용자가 말한 데이터를 ArrayList에 저장
            speak = list.get(0);//사용자의 말을 음성인식한 데이터를 String 형 speak변수에 저장
            speak = speak.replaceAll(" ","");// 사용자가 말한 문자열이 띄어쓰기가 있으면 공백 제거
        }

        @Override
        public void onPartialResults(Bundle partialResults) {

        }

        @Override
        public void onEvent(int eventType, Bundle params) {

        }
    };
    private void startLocationService(){//내 GPS를 이용하여 위도 경도 얻는 함수
        LocationManager locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);//위치관리자 생성

        GPSListener gpsListener = new GPSListener();
        long minTime = 5000;//단위 millisecond
        float minDistance = 10;//단위 m

        try{
            //locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,minTime,minDistance,gpsListener);;//gps
            //locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,minTime,minDistance,gpsListener);//네트워크
            //5초 마다 or 10m 이동할떄마다 업데이트   network는 gps에 비해 정확도가 떨어짐

            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            if(location!=null){
                latitude = location.getLatitude();
                longitude = location.getLongitude();
            }
        }catch (SecurityException e){
            e.printStackTrace();
        }
    }
    private class GPSListener implements LocationListener{//위치리너스 클래스

        @Override
        public void onLocationChanged(Location location) {
            Double latitude = location.getLatitude();
            Double longitude = location.getLongitude();

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    }//위도와 경도를 주소로 변환하는 클래스

    public String getAddress(double lat, double lng){
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> address = null;
        try{
            address = geocoder.getFromLocation(lat,lng,3);//7은 최대 결과
        } catch (IOException e) {
            e.printStackTrace();
        }

        Address address1 = address.get(0);
        return address1.getAddressLine(0).toString();
    }
    @Override
    protected void onDestroy () {
        super.onDestroy();

        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
}