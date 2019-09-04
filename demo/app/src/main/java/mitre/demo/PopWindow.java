package mitre.demo;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import org.w3c.dom.Text;


public class PopWindow extends Activity{
    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.popwindow);

        String name1 = getIntent().getExtras().getString("name1");
        int strength1 = getIntent().getExtras().getInt("strength1");

        String name2 = getIntent().getExtras().getString("name2");
        int strength2 = getIntent().getExtras().getInt("strength2");

        String name3 = getIntent().getExtras().getString("name3");
        int strength3 = getIntent().getExtras().getInt("strength3");

        TextView wifiname1 = findViewById(R.id.wifiname1);
        TextView wifiname2 = findViewById(R.id.wifiname2);
        TextView wifiname3 = findViewById(R.id.wifiname3);

        wifiname1.setText(name1);
        wifiname2.setText(name2);
        wifiname3.setText(name3);

        ImageView wifi1 = findViewById(R.id.ic_wifi1);
        ImageView wifi2 = findViewById(R.id.ic_wifi2);
        ImageView wifi3 = findViewById(R.id.ic_wifi3);

        setImage(wifi1, strength1);
        setImage(wifi2, strength2);
        setImage(wifi3, strength3);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        int width = dm.widthPixels;
        int height = dm.heightPixels;

        getWindow().setLayout((int)(width * 0.8), (int)(height * 0.6));

        wifiname1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent returnIntent = new Intent();
                returnIntent.putExtra("result","1"); // Send value back to MapsActivity
                setResult(Activity.RESULT_OK, returnIntent);
                finish();
            }
        });

        wifiname2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent returnIntent = new Intent();
                returnIntent.putExtra("result","2"); // Send value back to MapsActivity
                setResult(Activity.RESULT_OK, returnIntent);
                finish();
            }
        });

        wifiname3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent returnIntent = new Intent();
                returnIntent.putExtra("result","3"); // Send value back to MapsActivity
                setResult(Activity.RESULT_OK, returnIntent);
                finish();
            }
        });

        Button retry = findViewById(R.id.retry);

        retry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(PopWindow.this)
                        .setMessage("Do you want to test again?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startActivity(new Intent(PopWindow.this, MapsActivity.class)); //close the pop window
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .create().show();

            }
        });

    }

    // Show WiFi strength
    private void setImage(ImageView v, int strength) {
        if (strength > 66) {
            v.setImageResource(R.drawable.ic_excellent);
        } else if (strength >= 33 || strength <= 66) {
            v.setImageResource(R.drawable.ic_good);
        } else if (strength >= 0 || strength < 33) {
            v.setImageResource(R.drawable.ic_fair);
        } else {
            v.setImageResource(R.drawable.ic_poor);
        }
    }
}
