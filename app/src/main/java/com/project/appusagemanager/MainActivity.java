package com.project.appusagemanager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static android.app.AppOpsManager.OPSTR_GET_USAGE_STATS;
import static android.os.Process.myUid;
import static androidx.core.app.AppOpsManagerCompat.MODE_ALLOWED;

public class MainActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_PACKAGE_USAGE_STATS = 100;

    private List<UsageStats> lUsageStatsList;
    private List<AppUsage> appUsageList;

    FloatingActionButton fab;
    RecyclerView mRecyclerView;
    RecyclerView.Adapter mAdapter;
    RecyclerView.LayoutManager manager;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference appUsageRef = db.collection("AppUsages");
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fillStats();

        View view = findViewById(R.id.ma);
        fab=findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Snackbar.make(view,"Data usage is reloading...",Snackbar.LENGTH_SHORT).show();
                fillStats();
            }
        });

    }


    private void fillStats() {
        if (hasPermission()){
            getStats();
        }else{
            requestPermission();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("MainActivity", "resultCode " + resultCode);
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_PACKAGE_USAGE_STATS:
                fillStats();
                break;
        }
    }

    private void requestPermission() {
        Toast.makeText(this, "Need to request permission", Toast.LENGTH_SHORT).show();
        startActivityForResult(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS), MY_PERMISSIONS_REQUEST_PACKAGE_USAGE_STATS);
    }

    private boolean hasPermission() {
        AppOpsManager appOps = (AppOpsManager)
                getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
//        return ContextCompat.checkSelfPermission(this,
//                Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED;
    }

    private void getStats() {
        UsageStatsManager lUsageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        Calendar cal = Calendar.getInstance();
        //cal.add(Calendar.DATE, -1);
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        lUsageStatsList = lUsageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, cal.getTimeInMillis(), System.currentTimeMillis());
        Collections.sort(lUsageStatsList,new timeInForegroundComparator());
        mRecyclerView = findViewById(R.id.rv);
        mRecyclerView.setHasFixedSize(true);

        manager = new LinearLayoutManager(MainActivity.this, LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(manager);
        List<AppUsage> appList = new ArrayList<>();
        for (UsageStats lUsageStats : lUsageStatsList) {
            AppUsage appUsage = new AppUsage();
            appUsage.setName(lUsageStats.getPackageName());
            appUsage.setTiming(String.valueOf(lUsageStats.getTotalTimeInForeground()));
            appList.add(appUsage);

        }
        mAdapter = new ItemAdapter(this, appList);
        mRecyclerView.setAdapter(mAdapter);

    }
    private  class timeInForegroundComparator implements Comparator<UsageStats> {

        @Override
        public int compare(UsageStats left, UsageStats right) {
            return Long.compare(right.getTotalTimeInForeground(), left.getTotalTimeInForeground());
        }
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Closing AppUsage Manager")
                .setMessage("Are you sure you want to exit AppUsage Manager?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }

                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.upload:
                Toast.makeText(this, "Data uploaded", Toast.LENGTH_SHORT).show();
                appUsageList=new ArrayList<>();
                for (UsageStats lUsageStats : lUsageStatsList) {
                    AppUsage appUsage = new AppUsage();
                    appUsage.setName(lUsageStats.getPackageName());
                    appUsage.setTiming(String.valueOf(lUsageStats.getTotalTimeInForeground()));
                    appUsageRef.document(appUsage.getName()).set(appUsage);
                    appUsageList.add(appUsage);
                }
                return true;
            case R.id.delete:
                Toast.makeText(this, "Data deleted", Toast.LENGTH_SHORT).show();
                if (appUsageList.size()!=0) {
                    for (int i = 0; i < appUsageList.size(); i++) {
                        appUsageRef.document(appUsageList.get(i).getName()).delete();
                    }
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}