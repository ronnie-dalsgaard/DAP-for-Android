package rd.dap.activities;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import rd.dap.R;
import rd.dap.model.Audiobook;
import rd.dap.model.AudiobookManager;
import rd.dap.model.Bookmark;
import rd.dap.model.BookmarkManager;
import rd.dap.model.Track;
import rd.dap.services.HeadSetReceiver;
import rd.dap.services.PlayerService;
import rd.dap.services.PlayerService.DAPBinder;
import rd.dap.support.Monitor;
import rd.dap.support.Time;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Space;
import android.widget.TextView;

public class MainActivity extends Activity implements OnClickListener, ServiceConnection, PlayerService.PlayerObserver {
	private static final String TAG = "MainActivity";
	private static Drawable noCover, drw_play, drw_pause, drw_play_on_cover, drw_pause_on_cover;
	private static Monitor monitor;
	private PlayerService player;
	private boolean bound = false;
	private static final int CELL = 1111;
	private static final int BOOKMARK = 2222;
	private Menu menu;
	private Timer timer;
	private boolean timerOn = false;
	private final int TIMER_DELAY = 15;
	private final TimeUnit TIMER_UNIT = TimeUnit.SECONDS;
	private Monitor bookmark_monitor = null;
	private LinearLayout bookmark_list;

	//Activity + Bind to PlayerService
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.controller);

		//No cover
		if(noCover == null || drw_play == null || drw_pause == null
				|| drw_play_on_cover == null || drw_pause_on_cover == null){
			noCover = getResources().getDrawable(R.drawable.ic_action_help);
			drw_play = getResources().getDrawable(R.drawable.ic_action_play);
			drw_pause = getResources().getDrawable(R.drawable.ic_action_pause);
			drw_play_on_cover = getResources().getDrawable(R.drawable.ic_action_play_over_video);
			drw_pause_on_cover = getResources().getDrawable(R.drawable.ic_action_pause_over_video);
		}

		//Set stream for hardware volume buttons
		setVolumeControlStream(AudioManager.STREAM_MUSIC);

		// Request audio focus for playback
		final AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		final ComponentName receiver = new ComponentName(getPackageName(), HeadSetReceiver.class.getName());

		final OnAudioFocusChangeListener l = new OnAudioFocusChangeListener() {

			@Override
			public void onAudioFocusChange(int focusChange) {
				if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT){
					// Pause playback
				} else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
					am.registerMediaButtonEventReceiver(receiver);
					// Resume playback 
				} else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
					am.unregisterMediaButtonEventReceiver(receiver);
					am.abandonAudioFocus(this);
					// Stop playback
				}
			}
		};
		int result = am.requestAudioFocus(l, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
		System.out.println("Audiofocus: "+result);
		if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
			throw new RuntimeException("Unable to obtaing audio focus");
		}
		Log.d(TAG, "Audio focus gained");

		//Start playerservice
		Intent serviceIntent = new Intent(this, PlayerService.class);
		startService(serviceIntent);

		//Buttons
		ImageButton cover_btn = (ImageButton) findViewById(R.id.audiobook_basics_cover_btn);
		cover_btn.setImageDrawable(null);
		cover_btn.setOnClickListener(this);

		ImageButton next_btn = (ImageButton) findViewById(R.id.track_next);
		next_btn.setOnClickListener(this);

		ImageButton prev_btn = (ImageButton) findViewById(R.id.track_previous);
		prev_btn.setOnClickListener(this);

		ImageButton forward_btn = (ImageButton) findViewById(R.id.seeker_fast_forward);
		forward_btn.setOnClickListener(this);

		ImageButton rewind_btn = (ImageButton) findViewById(R.id.seeker_rewind);
		rewind_btn.setOnClickListener(this);
		
		bookmark_list = (LinearLayout) findViewById(R.id.controller_bookmark_list);

		//Current bookmark
		SharedPreferences pref = getPreferences(Context.MODE_PRIVATE);
		final String author = pref.getString("author", null);
		final String album = pref.getString("album", null);
		BookmarkManager bm = BookmarkManager.getInstance();
		Bookmark bookmark = bm.getBookmark(author, album);
		if(player != null && player.getAudiobook() == null) {
			Audiobook audiobook = AudiobookManager.getInstance().getAudiobook(bookmark);
			if(audiobook != null) {
				player.set(audiobook, bookmark.getTrackno(), bookmark.getProgress());
			}
		}
		/*
		 * Initially the bookmarks will not have been loaded at this point in time.
		 * Therefore the bookmark will also be displayed after bookmarks and 
		 * audiobooks are loaded. 
		 */

		//Load Audiobooks and Bookmarks
		new AsyncTask<Activity, Void, Bookmark>(){
			Activity activity;
			@Override
			protected Bookmark doInBackground(Activity... params) {
				activity = params[0];
				AudiobookManager.getInstance().loadAudiobooks(activity);
				BookmarkManager.getInstance().loadBookmarks(activity.getFilesDir()); 

				if(author != null && album != null){
					BookmarkManager bm = BookmarkManager.getInstance();
					Bookmark bookmark = bm.getBookmark(author, album);
					return bookmark;
				}
				return null;
			}
			@Override 
			protected void onPostExecute(final Bookmark bookmark){
				Log.d(TAG, "onPostExecute - audiobooks loaded");

				runOnUiThread(new Runnable() {

					@Override 
					public void run() {
						if(bookmark != null){
							if(player != null && player.getAudiobook() == null) {
								Audiobook audiobook = AudiobookManager.getInstance().getAudiobook(bookmark);
								if(audiobook != null) {
									player.set(audiobook, bookmark.getTrackno(), bookmark.getProgress());
								}
							}
						}
						displayBookmarks();
					}

				});
			}
		}.execute(this);

		//Start monitor
		if(monitor != null) monitor.kill();
		monitor = new displayMonitor(this);
		monitor.start();

	}
	@Override
	public void onStart(){
		Log.d(TAG, "onStart");
		super.onStart();
		//Bind to PlayerService
		Intent intent = new Intent(this, PlayerService.class);
		bindService(intent, this, Context.BIND_AUTO_CREATE);	
	}
	@Override
	public void onStop(){
		Log.d(TAG, "onStop");
		super.onStop();
		//Unbind from PlayerService
		if(bound){
			unbindService(this);
			bound = false;
		}
	}
	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		Log.d(TAG, "onServiceConnected");
		DAPBinder binder = (DAPBinder) service;
		player = binder.getPlayerService();
		bound = true;

		player.addObserver(this);

		Audiobook audiobook = player.getAudiobook();
		int trackno = player.getTrackno();
		if(audiobook != null){
			Track track  = audiobook.getPlaylist().get(trackno);
			displayInfo(audiobook, track);
			displayTracks(audiobook, track, trackno);
			displayTime(track);
		}
		displayPlayButton();
		
		if(bookmark_monitor != null) bookmark_monitor.kill();
		bookmark_monitor = new BookmarkMonitor();
		bookmark_monitor.start();
	}
	@Override
	public void onServiceDisconnected(ComponentName name) {
		bound = false;
		ImageButton cover_btn = (ImageButton) findViewById(R.id.audiobook_basics_cover_btn);
		if(cover_btn != null) cover_btn.setImageDrawable(null);
	}
	@Override
	public void onClick(View v) {
		//reused variables
		int currentProgress, progress;
		Audiobook audiobook;
		
		switch(v.getId()){
		case R.id.audiobook_basics_cover_btn: 
			if(player == null) break;
			player.toggle();
			break;
		case R.id.track_next:
			if(player == null) break;
			player.next();
			break;
		case R.id.track_previous: 
			if(player == null) break;
			player.prev();
			break;
		case R.id.seeker_fast_forward: 
			if(player == null) break;
			currentProgress = player.getCurrentProgress();
			progress = currentProgress + Time.toMillis(1, TimeUnit.MINUTES);
			int duration = player.getDuration();
			player.seekTo(Math.min(progress, duration));
			break;
		case R.id.seeker_rewind: 
			if(player == null) break;
			currentProgress = player.getCurrentProgress();
			progress = currentProgress - Time.toMillis(1, TimeUnit.MINUTES);
			player.seekTo(Math.max(progress, 0));
			break;
		case CELL:
			if(player == null) break;
			audiobook = player.getAudiobook();
			int trackno = (int) v.getTag();
			player.set(audiobook, trackno, 0);
			break;
		case BOOKMARK:
			Bookmark bookmark = (Bookmark) v.getTag();
			if(bookmark == null) break;
			audiobook = AudiobookManager.getInstance().getAudiobook(bookmark);
			if(audiobook == null) break;
			if(player == null) break;
			if(!audiobook.equals(player.getAudiobook())) {
				player.set(audiobook, bookmark.getTrackno(), bookmark.getProgress());
			}
			break;
		}
	}

	//Callbacks from PlayerService
	@Override
	public void set(final Audiobook audiobook, final int trackno, final int progress){
		runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				Track track = audiobook.getPlaylist().get(trackno);
				displayInfo(audiobook, track);
				displayPlayButton();
				displayTracks(audiobook, track, trackno);
				displayTime(track);
			}
		});	
	}
	@Override
	public void play() { 
		runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				ImageButton cover_btn = (ImageButton) findViewById(R.id.audiobook_basics_cover_btn);
				cover_btn.setImageDrawable(drw_pause_on_cover);
			}
		});
	}
	@Override
	public void pause() { 
		runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				ImageButton cover_btn = (ImageButton) findViewById(R.id.audiobook_basics_cover_btn);
				cover_btn.setImageDrawable(drw_play_on_cover);
			}
		});
	}
	@Override
	public void next(final Audiobook audiobook, final Track track, final int trackno) { 
		runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				displayTracks(audiobook, track, trackno);
				displayTime(track);
			}
		});
	}
	@Override
	public void prev(final Audiobook audiobook, final Track track, final int trackno) { 
		runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				displayTracks(audiobook, track, trackno);
				displayTime(track);
			}
		});
	}
	@Override
	public void seek(final Track track){
		runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				displayTime(track);
			}
		});
	}
	@Override
	public void complete(final Audiobook audiobook, final int new_trackno) {
		runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				if(new_trackno == -1) return;
				Track track = audiobook.getPlaylist().get(new_trackno);
				displayTracks(audiobook, track, new_trackno);
				displayTime(track);
			}
		});
	}
	@Override
	public void updateBookmark(String author, String album, int trackno, int progress){
		
	}

	//Update views
	private void displayInfo(Audiobook audiobook, Track track){

		//Author
		TextView author_tv = (TextView) findViewById(R.id.audiobook_basics_author_tv);
		if(author_tv != null) author_tv.setText(audiobook.getAuthor());

		//Album
		TextView album_tv = (TextView) findViewById(R.id.audiobook_basics_album_tv);
		if(album_tv != null) album_tv.setText(audiobook.getAlbum());

		//Cover
		ImageView cover_iv = (ImageView) findViewById(R.id.audiobook_basics_cover_iv);
		String cover = track.getCover();
		if(cover == null) cover = audiobook.getCover();
		if(cover != null) {
			Bitmap bitmap = BitmapFactory.decodeFile(cover);
			if(cover_iv != null) cover_iv.setImageBitmap(bitmap);
		} else {
			if(cover_iv != null) cover_iv.setImageDrawable(noCover);
		}
	}
	private void displayTracks(Audiobook audiobook, Track track, int trackno){
		TextView title_tv = (TextView) findViewById(R.id.track_title);
		LinearLayout tracks_gv = (LinearLayout) findViewById(R.id.controller_tracks_grid);
		if(audiobook == null){
			if(title_tv != null) title_tv.setText("Title");
			if(tracks_gv != null) tracks_gv.removeAllViews();
			return;
		}

		//Title
		String _trackno = String.format(Locale.US, "%02d", trackno+1);
		if(title_tv != null) title_tv.setText(_trackno + " " + track.getTitle());

		//Tracks
		if(tracks_gv != null){
			int trackCount = audiobook.getPlaylist().size();
			tracks_gv.removeAllViews();
			final int COLUMNS = 8;
			LinearLayout row = null;
			int m = LayoutParams.MATCH_PARENT;
			int w = LayoutParams.WRAP_CONTENT;
			LayoutParams row_p = new LinearLayout.LayoutParams(m, w);
			LayoutParams p = new LinearLayout.LayoutParams(0, 80, 1);
			for(int i = 0; i < trackCount; i++){
				if(i % COLUMNS == 0){
					row = new LinearLayout(this);
					row.setOrientation(LinearLayout.HORIZONTAL);
					tracks_gv.addView(row, row_p);
				}
				TextView cell = new TextView(this);
				cell.setTextColor(getResources().getColor(R.color.white));
				cell.setGravity(Gravity.CENTER);
				cell.setText(String.format("%02d", i+1));
				if(i == trackno){
					cell.setBackground(getResources().getDrawable(R.drawable.circle));
				}
				cell.setId(CELL);
				cell.setTag(i); //Autoboxing
				cell.setOnClickListener(this);
				row.addView(cell, p);
			}
			if(trackCount % COLUMNS > 0){
				Space space = new Space(this);
				int weight = COLUMNS - (trackCount % COLUMNS);
				LinearLayout.LayoutParams space_p = new LinearLayout.LayoutParams(0, 75, weight);
				row.addView(space, space_p);
			}
		}
	}
	private void displayTime(Track track){
		//Progress
		TextView progress_tv = (TextView) findViewById(R.id.seeker_progress_tv);

		if(player == null) {
			progress_tv.setText(Time.toString(0));			
		} else {
			int progress = player.getCurrentProgress();
			String _progress = Time.toString(progress);
			long duration = track.getDuration();
			String _duration = Time.toString(duration);
			if(duration > 0) _progress += " / " + _duration;
			progress_tv.setText(_progress);
		}
	}
	private void displayPlayButton(){
		//Cover button
		ImageButton cover_btn = (ImageButton) findViewById(R.id.audiobook_basics_cover_btn);
		if(player == null || player.getAudiobook() == null) {
			cover_btn.setImageDrawable(null);
		} else {
			cover_btn.setImageDrawable(player.isPlaying() ? drw_pause_on_cover : drw_play_on_cover);
		}
	}
	private void displayBookmarks(){
		if(bookmark_list == null) { System.out.println("no bookmark list!"); return; }
		bookmark_list.removeAllViews();
		LayoutInflater inflater = LayoutInflater.from(this);
		for(Bookmark bookmark : BookmarkManager.getBookmarks()){
			View v = inflater.inflate(R.layout.bookmark_item, bookmark_list, true);
			v.setId(BOOKMARK);
			v.setTag(bookmark);
			v.setOnClickListener(this);
			
			//Cover
			Audiobook audiobook = AudiobookManager.getInstance().getAudiobook(bookmark);
			if(audiobook != null){
				ImageView cover_iv = (ImageView) v.findViewById(R.id.bookmark_cover_iv);
				String cover = audiobook.getCover();
				if(cover != null) {
					Bitmap bitmap = BitmapFactory.decodeFile(cover);
					if(cover_iv != null) cover_iv.setImageBitmap(bitmap);
				} else {
					if(cover_iv != null) cover_iv.setImageDrawable(noCover);
				}
			}
			
			//Track no
			TextView track_tv = (TextView) v.findViewById(R.id.bookmark_track_tv);
			track_tv.setText(String.format("%02d", bookmark.getTrackno()));
			
			//Progress
			TextView progress_tv = (TextView) v.findViewById(R.id.bookmark_progress_tv);
			progress_tv.setText(Time.toString(bookmark.getProgress()));
		}
	}
	
	//Menu
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		this.menu = menu;
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		MenuItem item = menu.getItem(1);
		int delay = Time.toMillis(TIMER_DELAY, TIMER_UNIT);
		item.setTitle(Time.toString(delay));
		return true;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){
		case R.id.menu_item_timer: 
			if(!timerOn){
				MenuItem menuitem = menu.getItem(1);
				timer = new Timer(TIMER_DELAY, TIMER_UNIT, menuitem);
				timer.start();
				timerOn = true;
			} else {
				timer.kill();
			}
			break;
		}
		return true;
	}

	class displayMonitor extends Monitor {
		private Activity activity;

		public displayMonitor(Activity activity) {
			super(1, TimeUnit.SECONDS);
			this.activity = activity;
		}

		@Override
		public void execute() {
			if(activity == null) return;
			activity.runOnUiThread(new Runnable() {

				@Override
				public void run() {
					if(player == null) return;
					Audiobook audiobook = player.getAudiobook();
					if(audiobook == null) return;
					int trackno = player.getTrackno();
					Track track = audiobook.getPlaylist().get(trackno);
					displayTime(track);
				}
			});
		}

	}
	class Timer extends Monitor {
		private long endTime;
		private MenuItem item;
		private String _delay;
		
		public Timer(int delay, TimeUnit unit, final MenuItem item) {
			super(1, TimeUnit.SECONDS);
			
			delay = Time.toMillis(delay, unit);
			endTime = System.currentTimeMillis() + delay;
			
			this.item = item;
			_delay = Time.toString(delay);
			runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					item.setTitle(_delay);
				}
			});
		}

		@Override
		public void execute() {
			final long timeleft = endTime - System.currentTimeMillis();
			runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					item.setTitle(Time.toString(timeleft));
				}
			});
			if(timeleft <= 0){
				player.pause();
				kill();
			}
		}
		
		@Override
		public void kill(){
			runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					item.setTitle(_delay);
				}
			});
			timerOn = false;
			super.kill();
		}
	}
	class BookmarkMonitor extends Monitor {
		private static final String TAG = "Monitor_bookmarks";
		private boolean go_again = true;

		public BookmarkMonitor() {
			super(5, TimeUnit.SECONDS);
		}

		@Override
		public void execute() {
			if(player == null) return;

			if(!go_again && !player.isPlaying()){
				return;
			}
			
			Audiobook audiobook = player.getAudiobook();
			if(audiobook == null) return;
			
			String author = audiobook.getAuthor();
			String album = audiobook.getAlbum();
			int trackno = player.getTrackno();
			int progress = player.getCurrentProgress();
			boolean force = false; //only update bookmark if progress is greater than previously recorded
			BookmarkManager bm = BookmarkManager.getInstance();
			if(trackno > 0 || progress > 0){
				Bookmark bookmark = bm.createOrUpdateBookmark(getFilesDir(), author, album, trackno, progress, force);
				BookmarkManager.getInstance().saveBookmarks(getFilesDir());
				Log.d(TAG, "Bookmark created or updated\n"+bookmark);
				
				//Update view
				runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						displayBookmarks();
					}
				});
			}
			go_again = player.isPlaying();
		}
		
	}
}
