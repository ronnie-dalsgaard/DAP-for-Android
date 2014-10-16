package rd.dap;

import static rd.dap.AudiobookActivity.STATE_EDIT;
import static rd.dap.AudiobookActivity.STATE_NEW;
import static rd.dap.FileBrowserActivity.TYPE_FOLDER;

import java.io.File;
import java.util.Locale;

import rd.dap.fragments.AudiobookGridFragment;
import rd.dap.fragments.AudiobookGridFragment.ImageAdapter;
import rd.dap.fragments.BookmarkListFragment;
import rd.dap.fragments.BookmarkListFragment.BookmarkAdapter;
import rd.dap.fragments.ControllerFragment;
import rd.dap.fragments.FragmentMiniPlayer;
import rd.dap.fragments.FragmentMiniPlayer.MiniPlayerObserver;
import rd.dap.model.Audiobook;
import rd.dap.model.AudiobookManager;
import rd.dap.model.Bookmark;
import rd.dap.model.BookmarkManager;
import rd.dap.model.Data;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class MainActivity extends Activity implements ActionBar.TabListener, MiniPlayerObserver {
	private static final String TAG = "MainActivity";
	public static FragmentMiniPlayer miniplayer = null;
	private SectionsPagerAdapter sectionsPagerAdapter;
	private ViewPager viewPager;
	private static AudiobookGridFragment audiobookGridFragment;
	private static BookmarkListFragment bookmarkListFragment;
	private ControllerFragment controllerFragment;
	private static final int REQUEST_NEW_AUDIOBOOK = 9001;
	private static final int REQUEST_EDIT_AUDIOBOOK = 9002;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		Intent serviceIntent = new Intent(this, PlayerService.class);
		startService(serviceIntent);
		
		audiobookGridFragment = new AudiobookGridFragment();
		bookmarkListFragment = new BookmarkListFragment();
		controllerFragment = new ControllerFragment();

		// Set up the action bar.
		final ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		// Create the adapter that will return a fragment for each of the three
		// primary sections of the activity.
		sectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

		// Set up the ViewPager with the sections adapter.
		viewPager = (ViewPager) findViewById(R.id.pager);
		viewPager.setAdapter(sectionsPagerAdapter);

		// When swiping between different sections, select the corresponding
		// tab. We can also use ActionBar.Tab#select() to do this if we have
		// a reference to the Tab.
		viewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override public void onPageSelected(int position) {
				actionBar.setSelectedNavigationItem(position);
				
				if(position == 2){
					miniplayer.setVisibility(View.GONE);
				} else {
					miniplayer.setVisibility(Data.getAudiobook() == null ? View.GONE : View.VISIBLE);
				}
			}
		});

		// For each of the sections in the app, add a tab to the action bar.
		for (int i = 0; i < sectionsPagerAdapter.getCount(); i++) {
			actionBar.addTab(actionBar.newTab()
					.setText(sectionsPagerAdapter.getPageTitle(i))
					.setTabListener(this));
		}
		
		
		FragmentManager fm = getFragmentManager();
		miniplayer = (FragmentMiniPlayer) fm.findFragmentById(R.id.main_miniplayer);
		miniplayer.addObserver(this);
//		miniplayer.setVisibility(Data.getAudiobook() == null ? View.GONE : View.VISIBLE);
//		miniplayer.setVisibility(View.VISIBLE);
	}

	//Menu
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){
		case R.id.menu_item_new_audiobook:
			Log.d(TAG, "menu_item_new_audiobook");
			Intent intent = new Intent(this, FileBrowserActivity.class);
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
		switch(requestCode){
		case REQUEST_NEW_AUDIOBOOK:
			Log.d(TAG, "onActivityResult - REQUEST_NEW_AUDIOBOOK");
			if(data == null) return;
			String folder_path = data.getStringExtra("result");
			File folder = new File(folder_path);
			AudiobookManager manager = AudiobookManager.getInstance();
			Audiobook audiobook = manager.autoCreateAudiobook(folder, true);
			Intent intent = new Intent(this, AudiobookActivity.class);
			intent.putExtra("state", STATE_NEW);
			intent.putExtra("audiobook", audiobook);
			startActivityForResult(intent, REQUEST_EDIT_AUDIOBOOK);
			break;
		case REQUEST_EDIT_AUDIOBOOK:
			Log.d(TAG, "onActivityResult - REQUEST_EDIT_AUDIOBOOK");
			audiobookGridFragment.getAdapter().notifyDataSetChanged();
			bookmarkListFragment.getAdapter().notifyDataSetChanged(); //just in case the bookmark was there before the audiobook
		}
	}
	
	//Tabs
	@Override
	public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
		// When the given tab is selected, switch to the corresponding page in the ViewPager.
		viewPager.setCurrentItem(tab.getPosition());
	}
	@Override
	public void onTabUnselected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
	}
	@Override
	public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
	}

	@Override public void miniplayer_play() {
//		Toast.makeText(AudiobookListActivity.this, "Play on miniplayer", Toast.LENGTH_SHORT).show();
	}
	@Override public void miniplayer_pause() {
//		Toast.makeText(AudiobookListActivity.this, "Pause on miniplayer", Toast.LENGTH_SHORT).show();
	}
	@Override public void miniplayer_longClick() {
//		Toast.makeText(AudiobookListActivity.this, "Long click on miniplayer", Toast.LENGTH_SHORT).show();
	}
	@Override public void miniplayer_click() {
//		Toast.makeText(AudiobookListActivity.this, "Click on miniplayer", Toast.LENGTH_SHORT).show();
	}
	@Override public void miniplayer_seekTo(int progress){
		controllerFragment.displayValues();
		controllerFragment.displayTracks();
		controllerFragment.displayProgress();
	}
	
	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the sections/tabs/pages.
	 */
	public class SectionsPagerAdapter extends FragmentPagerAdapter {
		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			switch(position){
			case 0: return audiobookGridFragment;
			case 1: return bookmarkListFragment;
			case 2: return controllerFragment;
			}
			return null;
		}

		@Override public int getCount() { return 3; }

		@Override
		public CharSequence getPageTitle(int position) {
			Locale l = Locale.getDefault();
			switch (position) {
			case 0: return getString(R.string.tab1).toUpperCase(l);
			case 1: return getString(R.string.tab2).toUpperCase(l);
			case 2: return getString(R.string.tab3).toUpperCase(l);
			default: return "tab";
			}
		}
	}

	
	public static class ChangeAudiobookDialogFragment extends DialogFragment {
		public static final ChangeAudiobookDialogFragment newInstance(int position){
			ChangeAudiobookDialogFragment fragment = new ChangeAudiobookDialogFragment();
			Bundle bundle = new Bundle();
			bundle.putInt("position", position);
			fragment.setArguments(bundle);
			return fragment ;
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			final int position = getArguments().getInt("position");
			final Audiobook audiobook = Data.getAudiobooks().get(position);

			return new AlertDialog.Builder(getActivity())
			.setMessage("Change audiobook")
			.setPositiveButton("Edit audiobook", new DialogInterface.OnClickListener() {
				@Override public void onClick(DialogInterface dialog, int which) {
					Intent intent = new Intent(getActivity(), AudiobookActivity.class);
					intent.putExtra("state", STATE_EDIT);
					intent.putExtra("audiobook", audiobook);
					startActivity(intent);
				}
			})
			.setNegativeButton("Delete audiobook", new DialogInterface.OnClickListener() {
				@Override public void onClick(DialogInterface dialog, int which) {
					ConfirmDeleteDialogFragment frag = ConfirmDeleteDialogFragment.newInstance(position);
					frag.show(getFragmentManager(), "ConfirmDeleteDialogFragment");
				}
			})
			.create();
		}
	}
	public static class ConfirmDeleteDialogFragment extends DialogFragment {
		public static final ConfirmDeleteDialogFragment newInstance(int position){
			ConfirmDeleteDialogFragment fragment = new ConfirmDeleteDialogFragment();
			Bundle bundle = new Bundle();
			bundle.putInt("position", position);
			fragment.setArguments(bundle);
			return fragment ;
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			int position = getArguments().getInt("position");
			final Audiobook audiobook = Data.getAudiobooks().get(position);
			return new AlertDialog.Builder(getActivity())
			.setIcon(getActivity().getResources().getDrawable(R.drawable.ic_action_warning))
			.setMessage("Confirm delete "+audiobook.getAuthor() + " - " + audiobook.getAlbum())
			.setPositiveButton("Cancel", null) //Do nothing
			.setNegativeButton("Confirm", new DialogInterface.OnClickListener() {
				@Override public void onClick(DialogInterface dialog, int which) {
					//stop and un-set as current
					miniplayer.getPlayer().pause();
					Data.setAudiobook(null);
					Data.setTrack(null);
					Data.setPosition(-1);

					//update the miniplayers view
					miniplayer.updateView();

					//Remove the audiobook
					AudiobookManager.getInstance().removeAudiobook(getActivity(), audiobook);
					Bookmark bookmark = BookmarkManager.getInstance().getBookmark(audiobook);
					BookmarkManager.getInstance().removeBookmark(getActivity(), bookmark);
					Log.d(TAG, "Deleting Audiobook:\n"+audiobook);
					Log.d(TAG, "Deleting Bookmark:\n"+bookmark);

					//update the list
					ImageAdapter audiobookAdapter = audiobookGridFragment.getAdapter();
					if(audiobookAdapter != null) audiobookAdapter.notifyDataSetChanged();
					
					BookmarkAdapter bookmarkAdapter = bookmarkListFragment.getAdapter();
					if(bookmarkAdapter != null) bookmarkAdapter.notifyDataSetChanged();
					else System.out.println("BookmarkAdapter is null");
					
					for(Bookmark b : Data.getBookmarks()) System.out.println("%%"+b);
				}
			})
			.create();
		}
	}
}