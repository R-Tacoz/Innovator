package com.example.solve;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

/*
TODO
 - Other tasks(unimportant)
    - Customize buttons
        - Better buttons than just + and -, may need to wait until full app is developed to ensure design stays consistent
    - Animate screen/fragment change
 */

public class SetupMainActivity extends AppCompatActivity implements SetupActivitySelectFragment.OnDataPass, SetupGradeSelectFragment.OnDataPass {

    public static final String SHARED_PREFERENCES_FILE = "com.example.solve.preferenceFile";

    private final AppCompatActivity me = this;

    ProgressBar setupProgressBar;
    TextView setupTxt;

    boolean showSetup;
    int currSetupPage, gradeSelect, activitySelect;


    FirebaseAuth auth;
    SharedPreferences settings;

    static final String GRADE = "SelectGrade";
    static final String ACTIVITY = "SelectActivity";
    static final String SHOW_SETUP = "ShowSetup";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        auth = FirebaseAuth.getInstance();
        getUserDataFromFirebase();//have this set showSetup to true if there's no user data found
        //deleting the entry from database enables us to test setup

        if (!showSetup) {
            Intent mainMenuIntent = new Intent(me, MainMenuActivity.class);
            startActivity(mainMenuIntent);
            finish();
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.setup_activity_main);

        final Button gradeSelectNextBtn = findViewById(R.id.setupGradeNextBtn);
        Button backBtn = findViewById(R.id.backBtn);
        final FragmentTransaction fragTran1 = getSupportFragmentManager().beginTransaction();

        setupProgressBar = findViewById(R.id.setupProgressBar);
        setupProgressBar.getProgressDrawable().setColorFilter(Color.parseColor("#19A0FB"), android.graphics.PorterDuff.Mode.SRC_IN);
        setupTxt = findViewById(R.id.setupTxt);

        gradeSelect = 1;
        activitySelect = 1;

        if (findViewById(R.id.setupFragmentFrameLayout) != null) {
            SetupGradeSelectFragment gradeFragment = SetupGradeSelectFragment.newInstance(1);
            gradeFragment.setArguments(getIntent().getExtras());
            fragTran1.add(R.id.setupFragmentFrameLayout, gradeFragment);
            fragTran1.commit();
        }

        currSetupPage = 0;

        gradeSelectNextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                FragmentManager fragMan = getSupportFragmentManager();
                FragmentTransaction fragTran2 = fragMan.beginTransaction();

                //fragTran1.addToBackStack(null);
                if (currSetupPage < 2)
                    currSetupPage++;
                switch (currSetupPage) {
                    case 0:
                        fragTran2.replace(R.id.setupFragmentFrameLayout, SetupGradeSelectFragment.newInstance(gradeSelect));
                        setupTxt.setText(getString(R.string.setup_grade_select_text));
                        fragTran2.addToBackStack(null); //user can reverse transaction of replacing the setupFragmentFrameLayout
                        fragTran2.commit();
                        break;
                    case 1:
                        fragTran2.replace(R.id.setupFragmentFrameLayout, SetupActivitySelectFragment.newInstance(activitySelect));
                        setupTxt.setText(getString(R.string.setup_activity_select_text));
                        //fragTran1.addToBackStack("grade-to-activity");
                        fragTran2.addToBackStack(null);
                        fragTran2.commit();
                        break;
                    case 2:
                        storeUserDataToFirebase();
                        setSetupFinished();
                        /*
                        TODO
                         Remove println when done, used for debug
                         */
                        System.out.println(settings.getAll());
                        Intent mainMenuIntent = new Intent(me, MainMenuActivity.class);
                        startActivity(mainMenuIntent);
                        finish();
                        break;
                }

                setupProgressBar.setProgress(currSetupPage);
            }
        });

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //getSupportFragmentManager().popBackStack("grade-to-activity", 0);

                getSupportFragmentManager().popBackStack();
                if (currSetupPage > 0) {
                    currSetupPage--;
                    setupProgressBar.setProgress(currSetupPage);
                }

                switch (currSetupPage) {
                    case 0:
                        setupTxt.setText(getString(R.string.setup_grade_select_text));
                        break;
                    case 1:
                        setupTxt.setText(getString(R.string.setup_activity_select_text));
                        break;
                    case 2:

                        break;
                }

                //onBackPressed();

            }
        });



    }

    @Override
    public void onPause() {
        super.onPause();
        storeUserDataToFirebase();
    }

    public void setSetupFinished() {
        SharedPreferences.Editor edit = settings.edit();
        edit.putBoolean(SHOW_SETUP, false);
        edit.apply();
    }

    private void storeUserDataToFirebase(){
        String uid = auth.getUid();
        assert uid != null;
        DatabaseReference UserDataRef = FirebaseDatabase.getInstance().getReference().child("UserData").child(uid);
        ArrayList<Integer> data = new ArrayList<Integer>(2);
        data.set(0, activitySelect);
        data.set(1, gradeSelect);
        UserDataRef.setValue(data);
    }


    private void getUserDataFromFirebase(){
        FirebaseUser currentUser = auth.getCurrentUser();
        String uid = currentUser.getUid();
        assert uid != null;
        DatabaseReference UserDataRef = FirebaseDatabase.getInstance().getReference().child("UserData").child(uid);
        UserDataRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(!dataSnapshot.exists()){
                    showSetup = true;
                    return;
                }
                showSetup = false;
                ArrayList<Integer> result = dataSnapshot.getValue(new GenericTypeIndicator<ArrayList<Integer>>() {});
                if (result != null) {
                    activitySelect = result.get(0);
                    gradeSelect = result.get(1);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("FireBase getUserData","Failed");
            }
        });
    }


    @Override
    public void putGradeSelect(int gradeSelect) {//set
        this.gradeSelect = gradeSelect;
    }

    @Override
    public void putActivitySelect(int activitySelect) {
        this.activitySelect = activitySelect;
    }
}
