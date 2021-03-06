package com.example.cardinalPlanner.ToDos;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import com.example.cardinalPlanner.AlarmReceiver;
import com.example.cardinalPlanner.R;
import com.example.cardinalPlanner.ToDoMgmt;
import com.example.cardinalPlanner.model.ToDo;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import static com.example.cardinalPlanner.MainApplication.CHANNEL_1_ID;

/**
 * Class for the display where users can create TD objects and save them to the database
 */
public class CreateToDo extends AppCompatActivity {
    private FirebaseFirestore mFirestore;
    private EditText NameInput, timeInput, dateInput, descriptionInput;
    private RadioButton notificationsBtn, persistUntilCompletion;
    private CheckBox evryHr,evryDy;
    private boolean notifications = false;
    private boolean PUC = false;
    private Button finish;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
    private String TAG= "CreateToDo";
    private long notificationTime;
    private NotificationManagerCompat nm;
    private int NOTIFID = 0;
    /**
     * Initializes all UI elemts and fills in data from database if needed
     * @param savedInstanceState - called by android
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_to_do);

        NameInput = findViewById(R.id.NameInput);
        dateInput  = findViewById(R.id.eventDateInput);
        timeInput = findViewById(R.id.eventTimeInput);
        descriptionInput = findViewById(R.id.DescriptionInput);
        notificationsBtn = findViewById(R.id.NotificationsBtn);
        evryHr = findViewById(R.id.checkBoxHour);
        evryDy = findViewById(R.id.checkBoxDay);
        nm = NotificationManagerCompat.from(this);
        notificationsBtn.setOnClickListener(new View.OnClickListener() {
            /**
             * sets notification flag to true
             * @param view - cuirrent view
             */
            @Override
            public void onClick(View view) {
                notifications = true;
            }
        });
        persistUntilCompletion = findViewById(R.id.persistComplete);
        persistUntilCompletion.setOnClickListener(new View.OnClickListener() {
            /**
             * sets persistant flag to true
             * @param view - current view
             */
            @Override
            public void onClick(View view) {
                PUC = true;
            }
        });

        finish = findViewById(R.id.finishBtn);
        finish.setOnClickListener(new View.OnClickListener() {
            /**
             * Saves infor and sets up notifications if needed, for persistant the notification flag needs to be True as well
             * @param view - current view
             */
            @Override
            public void onClick(View view) {
                if(notifications){
                    String dayInfo[] = dateInput.getText().toString().split("-");
                    String timeInfo[] = timeInput.getText().toString().split(":");
                    Log.d(TAG, "onAddItemsClicked: "  + dayInfo[0]+"," + dayInfo[1] + "," + dayInfo[2]+ " | " + timeInfo[0] + "," +timeInfo[1] + "," +timeInfo[2] + ",");

                    Calendar calendar = Calendar.getInstance();
                    calendar.set(Calendar.YEAR,Integer.parseInt(dayInfo[0]));
                    calendar.set(Calendar.MONTH,Integer.parseInt(dayInfo[1])-1);
                    calendar.set(Calendar.DAY_OF_MONTH,Integer.parseInt(dayInfo[2]));

                    calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeInfo[0]));
                    calendar.set(Calendar.MINUTE, Integer.parseInt(timeInfo[1]));
                    calendar.set(Calendar.SECOND, Integer.parseInt(timeInfo[2]));
                    notificationTime = calendar.getTimeInMillis();
                    Log.d(TAG, "onClick: calendar:" + calendar.getTime());
                    Log.d(TAG, "onClick: Finish, sedding notication");
                    sendOnChannelOne();
                }
                onAddItemsClicked();
                Toast.makeText(CreateToDo.this, "New ToDo Created",
                        Toast.LENGTH_LONG).show();
            }
        });

    }

    /**
     * sets up notifications, and reminders for if they are persistant recurring or both
     */
    public void sendOnChannelOne(){
        Log.d(TAG, "sendOnChannelOne: Setting up notification");

            Intent newI = new Intent(getApplicationContext(), ToDoMgmt.class);
            PendingIntent pend = PendingIntent.getActivity(getApplicationContext(), 0, newI, 0);
        if(PUC){

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_1_ID)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setContentTitle("TODO: " + NameInput.getText().toString())
                    .setContentText("Description: " + descriptionInput.getText().toString())
                    .setContentIntent(pend)
                    .setWhen(notificationTime)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                    .setOnlyAlertOnce(false)
                    .setShowWhen(true)
                    .setOngoing(true)
                    .build();
            Log.d(TAG, "sendOnChannelOne: Creating Recurring notification with id: " +  Integer.toString((int)(notificationTime/1000)));
            nm.notify((int)(notificationTime/1000), notification);
        }else {
            Notification notification = new NotificationCompat.Builder(this, CHANNEL_1_ID)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setContentTitle("TODO: " + NameInput.getText().toString())
                    .setContentText("Description: " + descriptionInput.getText().toString())
                    .setContentIntent(pend)
                    .setWhen(notificationTime)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                    .setOnlyAlertOnce(false)
                    .setShowWhen(true)
                    .build();

            nm.notify((int)notificationTime, notification);
        }
        if(evryDy.isChecked() && !evryHr.isChecked()){
            Log.d(TAG, "sendOnChannelOne: Setting day alarm");
            Intent intent = new Intent(this, AlarmReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this.getApplicationContext(), 234324244, intent, 0);
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()
                    ,AlarmManager.INTERVAL_DAY, pendingIntent);
        }
        if(evryHr.isChecked()){
            Log.d(TAG, "sendOnChannelOne: setting hour alarm");
            Intent intent = new Intent(this, AlarmReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this.getApplicationContext(), 234324243, intent, 0);
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(),pendingIntent);
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()
                    ,AlarmManager.INTERVAL_HOUR, pendingIntent);

        }

    }
    /**
     * Convert String entered from user in the app to a Date to be passed to the firestore database
     * @param datetoSaved
     * @return date - user entered date for the event
     */
    public Date getDateFromString(String datetoSaved){
        try {
            java.util.Date date = format.parse(datetoSaved);
            return date ;
        } catch (ParseException e){
            return null ;
        }
    }

    /**
     * Take information input from the user and creates ToDos and posts it to the firebase database
     */
    public void onAddItemsClicked() {
        ToDo newToDo = new ToDo();
        String timestamp = dateInput.getText().toString() + "_" + timeInput.getText().toString();
        Date date = getDateFromString(timestamp);
        Log.d(TAG, "onAddItemsClicked: " + date.toString());
        String currentuser = FirebaseAuth.getInstance().getCurrentUser().getUid();
        newToDo.setDate(date);
        newToDo.setName(NameInput.getText().toString());
        newToDo.setPersistantUntilComplete(PUC);
        newToDo.setComplete(false);
        newToDo.setDescription(descriptionInput.getText().toString());
        newToDo.setNotification(notifications);
        newToDo.setUserId(currentuser);
        db.collection("toDo").add(newToDo);
    }
}