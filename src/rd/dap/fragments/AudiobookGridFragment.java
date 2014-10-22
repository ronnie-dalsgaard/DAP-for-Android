package rd.dap.fragments;

import static rd.dap.activities.AudiobookActivity.STATE_EDIT;
import static rd.dap.activities.AudiobookActivity.STATE_NEW;
import static rd.dap.activities.FileBrowserActivity.TYPE_FOLDER;
import static rd.dap.activities.MainActivity.miniplayer;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

import rd.dap.R;
import rd.dap.activities.AudiobookActivity;
import rd.dap.activities.FileBrowserActivity;
import rd.dap.model.Audiobook;
import rd.dap.model.AudiobookManager;
import rd.dap.model.Bookmark;
import rd.dap.model.BookmarkManager;
import rd.dap.model.Data;
import rd.dap.support.Changer;
import android.app.Activity;
import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class AudiobookGridFragment extends Fragment implements OnClickListener, OnLongClickListener {
	private static final String TAG = "AudiobookGridActivity";
	private LinearLayout layout;
	private static final int REQUEST_NEW_AUDIOBOOK = 9001;
	private static final int REQUEST_EDIT_AUDIOBOOK = 9002;
	private Changer changer;
	private Activity activity;
	private static final int COLUMNS = 3;

	//Fragment must-haves
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate");
		super.onCreate(savedInstanceState);



		//		adapter = new AudiobookAdapter(getActivity(), R.layout.cover_view, authors);

		setHasOptionsMenu(true);
	}
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.d(TAG, "onCreateView");

		layout = new LinearLayout(getActivity());
		layout.setOrientation(LinearLayout.VERTICAL);

		int width = (int) getResources().getDimension(R.dimen.mini_player_width);
		int height = LayoutParams.WRAP_CONTENT;
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(width, height);
		params.addRule(RelativeLayout.CENTER_HORIZONTAL);

		ScrollView scroller = new ScrollView(getActivity());
		scroller.addView(layout);

		RelativeLayout base = new RelativeLayout(getActivity());
		base.addView(scroller, params);

		displayAudiobooks();

		return base;
	}

	public void displayAudiobooks(){
		if(activity == null) return;
		if(layout != null){
			layout.removeAllViews();
			HashSet<String> authors_set = new HashSet<String>();
			for(Audiobook audiobook : Data.getAudiobooks()){
				authors_set.add(audiobook.getAuthor());
			}
			ArrayList<String> authors = new ArrayList<String>();
			authors.addAll(authors_set);


			//LayoutParams
			int width = LayoutParams.MATCH_PARENT;
			int height = LayoutParams.WRAP_CONTENT;
			LayoutParams table_params = new LayoutParams(width, height);
			int buttomMargin = (int) activity.getResources().getDimension(R.dimen.margin_big);
			table_params.setMargins(0, 0, 0, buttomMargin);

			LayoutParams row_params = new LayoutParams(width, height);

			width = (int)activity.getResources().getDimension(R.dimen.cover_width_big);
			height = (int)activity.getResources().getDimension(R.dimen.cover_height_big);
			LayoutParams cover_params = new LayoutParams(width, height);

			LayoutParams element_params = new LayoutParams(0, height, 1);

			for(String author : authors){
				TextView author_tv = new TextView(getActivity());
				author_tv.setTextAppearance(activity, android.R.style.TextAppearance_Large);
				author_tv.setTextColor(activity.getResources().getColor(R.color.white));
				author_tv.setText(author);
				layout.addView(author_tv);

				LinearLayout table = new LinearLayout(getActivity());
				table.setOrientation(LinearLayout.VERTICAL);

				ArrayList<Audiobook> books_by_author = new ArrayList<Audiobook>();

				for(Audiobook audiobook : Data.getAudiobooks()){
					if(author.equals(audiobook.getAuthor())){
						books_by_author.add(audiobook);
					}
				}		

				LinearLayout row = null;

				for(int i = 0; i < books_by_author.size(); i++){
					Audiobook audiobook = books_by_author.get(i);

					if(i % COLUMNS == 0){
						row = new LinearLayout(activity);
						row.setOrientation(LinearLayout.HORIZONTAL);
						table.addView(row, row_params);
					}

					ImageView cover_iv = new ImageView(activity);
					LinearLayout element = new LinearLayout(activity);
					element.setGravity(Gravity.CENTER_HORIZONTAL);
					element.setTag(audiobook);
					element.setOnClickListener(this);
					element.setOnLongClickListener(this);

					if(audiobook.getCover() != null){
						String cover = audiobook.getCover();
						Bitmap bm = BitmapFactory.decodeFile(cover);
						cover_iv.setImageBitmap(bm);
					} else {
						Drawable drw = activity.getResources().getDrawable(R.drawable.ic_action_help);
						cover_iv.setImageDrawable(drw);
					}

					element.addView(cover_iv, cover_params);
					row.addView(element, element_params);
				}

				int missing = COLUMNS - (books_by_author.size() % COLUMNS);
				if(missing < COLUMNS){
					for(int i = 0; i < missing; i++){
						View dummy = new View(activity);
						row.addView(dummy, element_params);
					}
				}

				layout.addView(table, table_params);
			}
		}
	}

	//Callback
	@Override 
	public void onAttach(Activity activity){
		super.onAttach(activity);
		this.activity = activity;
		try {
			changer = (Changer) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement Callback");
		}
	}

	//Menu
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		// Inflate the menu; this adds items to the action bar if it is present.
		inflater.inflate(R.menu.audiobooks, menu);
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){
		case R.id.menu_item_new_audiobook:
			Log.d(TAG, "menu_item_new_audiobook");
			Intent intent = new Intent(getActivity(), FileBrowserActivity.class);
			intent.putExtra("type", TYPE_FOLDER);
			intent.putExtra("message", "Select folder");
			intent.putExtra("requestcode", REQUEST_NEW_AUDIOBOOK);
			startActivityForResult(intent, REQUEST_NEW_AUDIOBOOK);
			break;

		}
		return super.onOptionsItemSelected(item);
	}
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data){
		Log.d(TAG, "onActivityResult");
		switch(requestCode){
		case REQUEST_NEW_AUDIOBOOK:
			Log.d(TAG, "onActivityResult - REQUEST_NEW_AUDIOBOOK");
			if(data == null) return;
			String folder_path = data.getStringExtra("result");
			File folder = new File(folder_path);
			AudiobookManager manager = AudiobookManager.getInstance();
			Audiobook audiobook = manager.autoCreateAudiobook(folder, true);
			Intent intent = new Intent(getActivity(), AudiobookActivity.class);
			intent.putExtra("state", STATE_NEW);
			intent.putExtra("audiobook", audiobook);
			startActivityForResult(intent, REQUEST_EDIT_AUDIOBOOK);
			break;
		case REQUEST_EDIT_AUDIOBOOK:
			Log.d(TAG, "onActivityResult - REQUEST_EDIT_AUDIOBOOK");

			//update the lists
			changer.updateAudiobooks();
			changer.updateBookmarks();
			changer.updateController();
		}
	}

	//Listeners

	@Override
	public void onClick(View view) {
		Log.d(TAG, "onItemClick");
		Audiobook audiobook = (Audiobook) view.getTag();
		Data.setCurrentAudiobook(audiobook);
		Data.setCurrentPosition(0);
		Data.setCurrentTrack(Data.getCurrentAudiobook().getPlaylist().get(Data.getCurentPosition()));

		changer.updateController();

		miniplayer.reload();
		miniplayer.updateView();
	}
	@Override
	public boolean onLongClick(View view) {
		Log.d(TAG, "onItemLongClick");
		Audiobook clicked = (Audiobook) view.getTag();
		changeAudiobookDialog(clicked);

		return true; //consume click
	}

	
	Dialog dialog;
	private void changeAudiobookDialog(final Audiobook audiobook){
		dialog = new Dialog(getActivity());
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		LayoutInflater inflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View dv = inflater.inflate(R.layout.dialog, layout, false);

		//Title
		TextView title_tv = (TextView) dv.findViewById(R.id.dialog_title_tv);
		title_tv.setText("Change audiobook");

		//Message
		TextView msg_tv = (TextView) dv.findViewById(R.id.dialog_msg_tv);
		msg_tv.setText(audiobook.getAuthor() + "\n" + audiobook.getAlbum());

		//Exit button
		ImageButton exit_btn = (ImageButton) dv.findViewById(R.id.dialog_exit_btn);
		exit_btn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
		});

		//Left button
		Button left_btn = (Button) dv.findViewById(R.id.dialog_left_btn);
		left_btn.setText("Delete");
		left_btn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				dialog.dismiss();
				deleteAudiobookDialog(audiobook);
			}
		});

		//Right button
		Button right_btn = (Button) dv.findViewById(R.id.dialog_right_btn);
		right_btn.setText("Edit");
		right_btn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				dialog.dismiss();

				Intent intent = new Intent(getActivity(), AudiobookActivity.class);
				intent.putExtra("state", STATE_EDIT);
				intent.putExtra("audiobook", audiobook);
				startActivityForResult(intent, REQUEST_EDIT_AUDIOBOOK); 
			}
		});


		dialog.setContentView(dv);
		dialog.show();
	}
	private void deleteAudiobookDialog(final Audiobook audiobook){
		dialog = new Dialog(getActivity());
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		LayoutInflater inflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View dv = inflater.inflate(R.layout.dialog, layout, false);

		//Title
		TextView title_tv = (TextView) dv.findViewById(R.id.dialog_title_tv);
		title_tv.setText("Delete audiobooke");

		//Message
		TextView msg_tv = (TextView) dv.findViewById(R.id.dialog_msg_tv);
		msg_tv.setText(audiobook.getAuthor() + "\n" + audiobook.getAlbum());

		//Exit button
		ImageButton exit_btn = (ImageButton) dv.findViewById(R.id.dialog_exit_btn);
		exit_btn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
		});

		//Left button
		Button left_btn = (Button) dv.findViewById(R.id.dialog_left_btn);
		left_btn.setText("Cancel");
		left_btn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
		});

		//Right button
		Button right_btn = (Button) dv.findViewById(R.id.dialog_right_btn);
		right_btn.setText("Confirm");
		right_btn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				dialog.dismiss();
				
				if(audiobook.equals(Data.getCurrentAudiobook())){
					if(miniplayer != null){
						//stop and un-set as current
						miniplayer.getPlayer().pause();
						Data.setCurrentAudiobook(null);
						Data.setCurrentTrack(null);
						Data.setCurrentPosition(-1);

						//update the miniplayers view
						miniplayer.updateView();
					}
				}

				//Remove the audiobook
				AudiobookManager.getInstance().removeAudiobook(getActivity(), audiobook);
				Bookmark bookmark = BookmarkManager.getInstance().getBookmark(audiobook);
				BookmarkManager.getInstance().removeBookmark(getActivity(), bookmark);
				Log.d(TAG, "Deleting Audiobook:\n"+audiobook);
				Log.d(TAG, "Deleting Bookmark:\n"+bookmark);
				
				changer.updateAudiobooks();
				changer.updateBookmarks();
				changer.updateController();
			}
		});

		dialog.setContentView(dv);
		dialog.show();
	}
	@Override
	public void onPause(){
		Log.d(TAG, "onPause");
		super.onPause();
		if(dialog == null) return;
		dialog.dismiss();
	}
}
