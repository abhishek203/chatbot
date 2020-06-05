package com.example.chatbot;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

class QuestionListAdpater extends ArrayAdapter<Question> {

    private Context mContext;
    int mResource;

    public QuestionListAdpater(@NonNull Context context, int resource, @NonNull List<Question> objects) {

        super(context, resource, objects);
        mContext = context;
        mResource = resource;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        String question = getItem(position).getQuestion();
        String answer = getItem(position).getAnswer();

        Question pair = new Question(question,answer);

        LayoutInflater inflater = LayoutInflater.from(mContext);
        convertView = inflater.inflate(mResource,parent,false);
        TextView que = convertView.findViewById(R.id.question);
        TextView ans = convertView.findViewById(R.id.answer);
        que.setText(question);
        ans.setText(answer);
        return  convertView;
    }
}
