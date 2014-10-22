package rd.dap.support;

import rd.dap.fragments.FragmentMiniPlayer;

public interface Changer {
	public FragmentMiniPlayer getMiniplayer();
	public void updateAudiobooks();
	public void updateBookmarks();
	public void updateController();

}