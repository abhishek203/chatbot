package com.example.chatbot;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

        private ImageButton mRecordBtn;
        private EditText mText;
        private ImageButton mSendBtn;
        private TextView mQuestion;
        private TextView mAnswer;
        private HashMap<String, Integer> inp_data = new HashMap<String, Integer>();
        private HashMap<Integer,String> out_data = new HashMap<>();
        private int maxlen=7;
        private Interpreter tflite;
        private TextToSpeech t;


    @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);
            mRecordBtn = (ImageButton) findViewById(R.id.record_btn);
            mText = findViewById(R.id.send_msg);
            mSendBtn = (ImageButton) findViewById(R.id.send_btn);
            final ListView listView = (ListView)findViewById(R.id.listView);
            final ArrayList<Question> listQuestion = new ArrayList<>();
            t = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if(status!=TextToSpeech.ERROR){
                        t.setLanguage(Locale.ENGLISH);
                    }
                }
            });

        try {
            tflite = new Interpreter(loadModelFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
                JSONObject jsonobject = new JSONObject(loadJSONFromAssets("inp_tokenizer.json"));
                Iterator<String> iterator = jsonobject.keys();
                while (iterator.hasNext()){
                    String key = iterator.next();
                    inp_data.put(key, (Integer) jsonobject.get(key));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                JSONObject out = new JSONObject(loadJSONFromAssets("out_tokenizer.json"));
                Iterator<String> iter = out.keys();
                while (iter.hasNext()){
                    String key1 = iter.next();
                    out_data.put(Integer.parseInt(key1),out.get(key1).toString());
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

        mSendBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    String Data = mText.getText().toString();
                    ArrayList<Integer> input1 = tokenize(Data.toLowerCase().trim());
                    float[] input2 = padSeq(input1);
                    String answer = "Hello";
                    try {
                        int prediction = doInference(input2);
                        answer = out_data.get(prediction);
                        Question qnew = new Question(Data,answer);
                        listQuestion.add(qnew);
                    } catch (IOException e) {
                        Question qnew = new Question(Data,"Sorry, I could not Understand You");
                        listQuestion.add(qnew);
                    }
                    QuestionListAdpater adapter = new QuestionListAdpater(MainActivity.this,R.layout.row,listQuestion);
                    listView.setAdapter(adapter);

                    String toSpeak = answer;
                    t.speak(toSpeak,TextToSpeech.QUEUE_FLUSH,null);
                    mText.getText().clear();
                }
            });
        }
        public void onPause(){
            if(t!=null){
                t.stop();
                t.shutdown();
            }
            super.onPause();
        }

    private int doInference(float[] data) throws IOException {
            float[][] inputs =new float[1][7];
            inputs[0] = data;
            float[][] outputs = new float[1][9];
            tflite.run(inputs,outputs);
            float[] inference = outputs[0];

            if ( inference == null || inference.length == 0 ) return -1;

            int largest = 0;
            for ( int i = 1; i < inference.length; i++ )
            {
                if ( inference[i] > inference[largest] ) largest = i;
            }
            return largest;
    }

        public String loadJSONFromAssets(String filename){
            String json;
            try {
                InputStream is = getAssets().open(filename);
                int size = is.available();
                byte[] buffer = new byte[size];
                is.read(buffer);
                is.close();
                json = new String(buffer);


            } catch (IOException e) {
                e.printStackTrace();
                return  null;
            }
            return json;
        }

        public ArrayList<Integer> tokenize(String message){
            List<String> parts = Arrays.asList(message.split(" " ));
            ArrayList<Integer> tokenizedmessage = new ArrayList<Integer>();

            for (String part:parts){

                if(part.trim()!= ""){
                    int index = 0;
                    if(inp_data.get(part)==null){
                        index=0;
                    }
                    else {
                        index = inp_data.get(part);
                    }
                    tokenizedmessage.add(index);
                }
            }
            return tokenizedmessage;
        }

        public float[] padSeq(ArrayList<Integer> sequence){

        if ( sequence.size() < maxlen ) {
                float[] array = new float[maxlen];
                for(int i=0;i<maxlen;i++){
                    if (i<sequence.size()) {
                        array[i] = (float) sequence.get(i);
                    }
                    else {
                        array[i]=0f;
                    }
                }
                return array;
            }
        else {
            float[] array = new float[maxlen];
            for (int i = 0;i<maxlen;i++){
                array[i]=(float) sequence.get(i);
            }
            return array;
        }
        }

    public MappedByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor assetFileDescriptor = this.getAssets().openFd("chatbot_small_1.tflite");
        FileInputStream fileInputStream = new FileInputStream(assetFileDescriptor.getFileDescriptor());
        FileChannel fileChannel = fileInputStream.getChannel();
        long startoffset = assetFileDescriptor.getStartOffset();
        long declaredLength = assetFileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startoffset, declaredLength);
    }


    public void getSpeechInput(View view) {

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, 10);
        } else {
            Toast.makeText(this, "Your Device Don't Support Speech Input", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case 10:
                if (resultCode == RESULT_OK && data != null) {
                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    mText.setText(result.get(0));
                }
                break;
        }
    }
}
