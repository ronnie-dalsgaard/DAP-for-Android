package rd.dap;

import static rd.dap.InputActivity.REQUEST_EDIT_COVER;
import static rd.dap.InputActivity.REQUEST_EDIT_TRACK_TITLE;
import static rd.dap.InputActivity.REQUEST_EDIT_TRACK_FILE;

import java.io.File;
import java.util.ArrayList;

import rd.dap.model.Audiobook;
import rd.dap.model.Track;
import rd.dap.support.Time;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class TrackActivity extends Activity {
	private Audiobook audiobook;
	private int position;
	private TextView position_tv, title_tv, duration_tv, file_tv;
	private ImageView cover_iv;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.track);

		//Verify input
		audiobook = (Audiobook) getIntent().getSerializableExtra("audiobook");
		if(audiobook == null) throw new RuntimeException("No audiobook supplied");

		position = getIntent().getIntExtra("position", -1);
		if(position == -1) throw new RuntimeException("No position supplied");

		//Track
		final Track track = audiobook.getPlaylist().get(position);

		//Position
		position_tv = (TextView) findViewById(R.id.track_position);
		position_tv.setText(String.format("%02d", position+1));

		//Title
		title_tv = (TextView) findViewById(R.id.track_title);
		title_tv.setText(track.getTitle());
		title_tv.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				ArrayList<String> list = new ArrayList<String>();
//				list.add("Dennis J�rgensen");
//				list.add("Rick Riordan");
//				list.add("John G. Hemry");
				Intent intent = new Intent(TrackActivity.this, InputActivity.class);
				intent.putExtra("list", list);
				intent.putExtra("value", track.getTitle());
				intent.putExtra("requestcode", REQUEST_EDIT_TRACK_TITLE);
				startActivityForResult(intent, REQUEST_EDIT_TRACK_TITLE);
			}
		});

		//Duration
		duration_tv = (TextView) findViewById(R.id.track_duration);
		if(track.getDuration() >= 0){
			String _duration = Time.toShortString(track.getDuration());
			duration_tv.setText(_duration);
			duration_tv.setVisibility(View.VISIBLE);
		} else {
			duration_tv.setVisibility(View.GONE);
		}

		//File
		file_tv = (TextView) findViewById(R.id.track_file);
		file_tv.setText(track.getFile().getPath());
		file_tv.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(TrackActivity.this, FileBrowserActivity.class);
				intent.putExtra("type", "audio");
				intent.putExtra("requestcode", REQUEST_EDIT_TRACK_FILE);
				startActivityForResult(intent, REQUEST_EDIT_TRACK_FILE);
			}
		});

		//Cover
		cover_iv = (ImageView) findViewById(R.id.track_cover);
		if(track.getCover() != null){
			Bitmap bm = BitmapFactory.decodeFile(track.getCover().getAbsolutePath());
			cover_iv.setImageBitmap(bm);
			cover_iv.setVisibility(View.VISIBLE);
		} else {
			cover_iv.setVisibility(View.GONE);
		}
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data){
		if(data == null) return;
		String result;
		switch(requestCode){
		case REQUEST_EDIT_TRACK_TITLE:
			result = data.getStringExtra("result");
			audiobook.getPlaylist().get(position).setTitle(result);
			title_tv.setText(result);
			break;
		case REQUEST_EDIT_TRACK_FILE:
			result = data.getStringExtra("result");
			File file = new File(result);
			audiobook.getPlaylist().get(position).setFile(file);
			file_tv.setText(file.getPath());
			break;
		}
	}
}