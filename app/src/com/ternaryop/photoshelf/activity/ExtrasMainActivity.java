package com.ternaryop.photoshelf.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.ternaryop.photoshelf.R;

public class ExtrasMainActivity extends PhotoTumblrActivity implements OnClickListener {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_extras_main);
		setupUI();
	}
	
	private void setupUI() {
		for (int buttonId : new int[] {
				R.id.test_page_button,
				R.id.birthdays,
				R.id.birthdays_publisher}) {
			Button button = (Button)findViewById(buttonId);
			button.setOnClickListener(this);
		}
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.test_page_button:
	    	ImagePickerActivity.startImagePicker(this, getString(R.string.test_page_url));
			break;
        case R.id.birthdays:
            BirthdaysActivity.startBirthdaysActivity(this, getBlogName());
            break;
        case R.id.birthdays_publisher:
            BirthdaysPublisherActivity.startBirthdayPublisherActivity(this, getBlogName());
            break;
		}
	}

    public static void startExtrasActivity(Context context) {
        Intent intent = new Intent(context, ExtrasMainActivity.class);
        Bundle bundle = new Bundle();
        intent.putExtras(bundle);

        context.startActivity(intent);
    }
}
