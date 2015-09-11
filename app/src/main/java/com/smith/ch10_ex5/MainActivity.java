package com.smith.ch10_ex5;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import static android.content.SharedPreferences.*;


public class MainActivity extends Activity
implements OnClickListener, OnItemClickListener {

    private TextView messageTextView;
    private TextView titleTextView;
    private ListView itemListView;
    private ListView listview_item;
    private Button startButton;
    private Button stopButton;

    private  long startMillis = System.currentTimeMillis();
    private boolean canceled = false;
    private int pausedSeconds;

    private RSSFeed feed;
    private FileIO io;
    private ListView itemsListView;
    private int downloadFeed = 0;
    private SharedPreferences mPrefs;
    private String context = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        io = new FileIO(getApplicationContext());

        titleTextView = (TextView) findViewById(R.id.titleTextView);
        itemsListView = (ListView) findViewById(R.id.itemsListView);

        itemsListView.setOnItemClickListener(this);

        new DownloadFeed().execute();

        messageTextView = (TextView) findViewById(R.id.messageTextView);

        startButton = (Button) findViewById(R.id.startButton);
        stopButton = (Button) findViewById(R.id.stopButton);

        startButton.setOnClickListener(this);
        stopButton.setOnClickListener(this);
        startTimer();

        mPrefs = getSharedPreferences("mPrefs", MODE_PRIVATE);
        downloadFeed = mPrefs.getInt("downloadFeed", downloadFeed++);

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View v,
                            int position, long id) {

        // get the item at the specified position
        RSSItem item = feed.getItem(position);

        // create an intent
        Intent intent = new Intent(this, ItemActivity.class);

        intent.putExtra("pubdate", item.getPubDate());
        intent.putExtra("title", item.getTitle());
        intent.putExtra("description", item.getDescription());
        intent.putExtra("link", item.getLink());

        this.startActivity(intent);
    }

    class DownloadFeed extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            io.downloadFile();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            Log.d("News reader", "Feed downloaded");
            new ReadFeed().execute();
            downloadFeed++;
        }
    }

    class ReadFeed extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            feed = io.readFile();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            Log.d("News reader", "Feed read");

            // update the display for the activity
            updateDisplay();
        }
    }

    public void updateDisplay()
    {
        if (feed == null) {
            titleTextView.setText("Unable to get RSS feed");
            return;
        }

        // set the title for the feed
        titleTextView.setText(feed.getTitle());

        // get the items for the feed
        ArrayList<RSSItem> items = feed.getAllItems();

        // create a List of Map<String, ?> objects
        ArrayList<HashMap<String, String>> data =
                new ArrayList<HashMap<String, String>>();
        for (RSSItem item : items) {
            HashMap<String, String> map = new HashMap<String, String>();
            map.put("date", item.getPubDateFormatted());
            map.put("title", item.getTitle());
            data.add(map);
        }

        // create the resource, from, and to variables
        int resource = R.layout.listview_item;
        String[] from = {"date", "title"};
        int[] to = {R.id.pubDateTextView, R.id.titleTextView};

        // create and set the adapter
        SimpleAdapter adapter =
                new SimpleAdapter(this, data, resource, from, to);
        itemsListView.setAdapter(adapter);

        Log.d("News reader", "Feed displayed");
    }

    private void startTimer() {
        final String FILENAME = "news_feed.xml";

        startMillis = System.currentTimeMillis();
        Timer timer = new Timer(true);
        TimerTask task = new TimerTask() {
            
            @Override
            public void run() {
                long elapsedMillis = System.currentTimeMillis() - startMillis;
                updateView(elapsedMillis);



            }
        };

        timer.schedule(task, 0, 1000);
    }


    public void onPause() {
        Editor editor = mPrefs.edit();
        editor.putInt("downloadFeed", downloadFeed);
        editor.commit();
        super.onPause();
    }

    @Override
    protected void onDestroy() {

        Editor editor = mPrefs.edit();
        editor.putInt("downloadFeed", downloadFeed);
        editor.commit();
        super.onDestroy();

    }


    @Override
    protected void onResume() {
        super.onResume();

        downloadFeed = mPrefs.getInt("downloadFeed", downloadFeed++);


        startMillis = System.currentTimeMillis();
        final Timer timer = new Timer(true);
        TimerTask task = new TimerTask() {

            @Override
            public void run() {
                    long elapsedMillis = System.currentTimeMillis() - startMillis;
                    updateView(elapsedMillis);
            }

        };


        timer.schedule(task, 0, 1000);
    }

    private void updateView(final long elapsedMillis) {

        messageTextView.post(new Runnable() {

            int elapsedSeconds = (int) elapsedMillis/ 1000;

            @Override
            public void run() {
                    if(!canceled) {
                        Thread.currentThread().run();
                        messageTextView.setText("Seconds: " + (elapsedSeconds) + System.getProperty("line.separator") + "File has downloaded " + downloadFeed + " times.");

                    }
                    if(canceled){
                        Thread.currentThread().interrupt();
                    }

            }
        });
    }

    @Override
    public void onClick(View v) {
        int lastSecond = 0;

        switch (v.getId()){
            case R.id.stopButton:
                canceled = true;
                break;
            case R.id.startButton:
                canceled = false;
                startTimer();
                break;
        }

    }
}