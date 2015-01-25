package souch.smp;

import android.media.MediaPlayer;
import android.os.SystemClock;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.robotium.solo.Solo;

import junit.framework.Assert;

import java.lang.reflect.Field;
import java.util.ArrayList;

public class MainTest extends ActivityInstrumentationTestCase2<Main> {
    private Solo solo;
    private Main main;

    private final int maxAcceptableUserDelay = 2000;
    private final int minSong = 4;
    // posSong of the prev test
    private int prevPosSong;

    public MainTest() {
        super(Main.class);
    }

    public void setUp() throws Exception {
        super.setUp();
        main = getActivity();
        solo = new Solo(getInstrumentation(), main);
        Log.d("MainTest", "====================================");
        prevPosSong = -1;
    }


    // simple test of playing the first song
    public void test1PlayOneSong() throws Exception {
        checkEnoughSong();
        checkLoadPref();

        //changeNbSong(1);
        int linePos = 1;
        solo.scrollToTop();
        solo.clickInList(linePos);

        // gives the whole thing 2 second to start
        checkPlayOk(linePos, true);

        // set the song pos for the next test of loadpref
        prevPosSong = getMusicSrv().getSong();
    }


    // the the curr play icon is shown at the right pos
    public void test2PlayButton() throws Exception {
        checkEnoughSong();
        checkLoadPref();

        int linePos = 3;
        solo.scrollToTop();
        solo.clickInList(linePos);
        // gives the whole thing 2 second to start
        checkPlayOk(linePos, true);

        // pause
        clickOnButton(R.id.play_button);
        checkPlayOk(linePos, false);

        // next should go to next and unpause
        clickOnButton(R.id.next_button);
        linePos++;
        checkPlayOk(linePos, true);

        clickOnButton(R.id.next_button);
        linePos++;
        checkPlayOk(linePos, true);

        clickOnButton(R.id.prev_button);
        linePos--;
        checkPlayOk(linePos, true);

        solo.scrollToTop();
        solo.clickInList(1);
        linePos = 1;
        checkPlayOk(linePos, true);
        // going backward at the top go to the bottom
        clickOnButton(R.id.prev_button);
        linePos = getNbSong() - 1;
        checkPlayOk(linePos, true);

        clickOnButton(R.id.goto_button);
        checkPlayOk(linePos, true);
        ListView songList = (ListView) solo.getView(R.id.song_list);
        Assert.assertTrue(songList.getCount() - 1 == songList.getLastVisiblePosition());

        // going forward at the bottom go to the top
        clickOnButton(R.id.next_button);
        linePos = 0;
        checkPlayOk(linePos, true);

        clickOnButton(R.id.goto_button);
        checkPlayOk(linePos, true);
        Assert.assertTrue(0 == songList.getFirstVisiblePosition());

        // pick a song
        linePos = 4;
        solo.clickInList(linePos);
        checkPlayOk(linePos, true);

        prevPosSong = linePos;
    }

    // todo: see if the listview update curr pause when musicservice goes to next song automatically.

    // todo: seekbar tests

    public void testZLoadPref() throws Exception {
        checkEnoughSong();
        checkLoadPref();
    }

    public void testPlayerState() throws Exception {
        PlayerState ps = new PlayerState();
        Assert.assertTrue(ps.getState() == PlayerState.Nope);
        Assert.assertTrue(ps.compare(PlayerState.Nope));
        ps.setState(PlayerState.Initialized);
        Assert.assertTrue(ps.compare(PlayerState.Nope | PlayerState.Initialized | PlayerState.End));
        Assert.assertFalse(ps.compare(PlayerState.Nope | PlayerState.End));
    }

    private void clickOnButton(int id) {
        // this does not work:
        //solo.clickOnButton(R.id.play_button);
        // this works:
        solo.clickOnView(solo.getView(id));
    }

    // test that there is enough song for performing other tests
    public void checkEnoughSong() throws Exception {
        int nbSong = getNbSong();
        Log.d("MainTest", "songs size: " + nbSong);
        Assert.assertTrue(nbSong >= minSong);
    }

    private int getNbSong() throws Exception {
        Field field = main.getClass().getDeclaredField("songs");
        field.setAccessible(true);
        ArrayList<Song> songList = (ArrayList<Song>) field.get(main);
        return songList.size();
    }

    // reduce the number of song available
    private void changeNbSong(int nbSong) throws Exception {
        Assert.assertTrue(nbSong >= 0);
        Assert.assertTrue(nbSong <= minSong);

        Field field = main.getClass().getDeclaredField("songs");
        field.setAccessible(true);
        ArrayList<Song> songList = (ArrayList<Song>) field.get(main);
        Assert.assertTrue(nbSong <= songList.size());

        while(songList.size() > nbSong) {
            songList.remove(songList.size() - 1);
        }
        field.set(main, songList);
    }

    // check that the curr icon is well set
    // linePos start from 1
    private void checkPlayOk(int linePos, boolean isPlaying) throws Exception {
        Log.d("MainTest", "checkPlayOk linePos:" + linePos + " playing: " + isPlaying);
        SystemClock.sleep(maxAcceptableUserDelay);
        Assert.assertTrue(getMusicSrv().isPlaying() == isPlaying);


        // check the play button (play or pause)
        int ic_action = R.drawable.ic_action_pause;
        if(!isPlaying)
            ic_action = R.drawable.ic_action_play;
        // this make the listview scroll forever and make the test fail. don't know why :
        //Assert.assertTrue(((int) solo.getButton(R.id.play_button).getTag()) == ic_action);
        // this works
        Assert.assertTrue(((int) (solo.getView(R.id.play_button)).getTag()) == ic_action);


        // check the image that show the current song (played or paused) in the list
        int songPos = linePos > 0 ? linePos - 1 : linePos; // clickInList start from 1, getChildAt from 0
        //Log.d("MainTest", "ic:" + R.drawable.ic_curr_pause + " - play:" + R.drawable.ic_curr_play + " - trans:" + R.drawable.ic_transparent);
        ListView songList = (ListView) solo.getView(R.id.song_list);
        int i;
        Log.d("MainTest", "songList.getCount():" + songList.getCount() + " songList.firstpos:" + songList.getFirstVisiblePosition() + " lastpos: " + songList.getLastVisiblePosition());
        for(i = songList.getFirstVisiblePosition(); i < songList.getLastVisiblePosition(); i++) {
            RelativeLayout songItem = (RelativeLayout) songList.getChildAt(i);
            // fixme: I don't understand why songItem is null
            if(songItem == null) {
                Log.w("MainTest", "!!! songItem null; i:" + i);
                continue;
            }
            ImageView currPlay = (ImageView) songItem.findViewById(R.id.curr_play);

            /*
            Log.d("MainTest", "i: " + i);
            Log.d("MainTest", "currPlay.getTag(): " + currPlay.getTag());
            TextView title = (TextView) songItem.findViewById(R.id.song_title);
            Log.d("MainTest", "title: " + title.getText());
            */

            int ic_curr = R.drawable.ic_curr_play;
            if(!isPlaying)
                ic_curr = R.drawable.ic_curr_pause;

            Assert.assertTrue(((int) currPlay.getTag()) == (i != songPos ? R.drawable.ic_transparent : ic_curr));
        }
    }

    public void checkLoadPref() throws Exception {
        if(prevPosSong != -1) {
            // check the the song is put at the last pos the app was
            ListView songList = (ListView) solo.getView(R.id.song_list);
            Assert.assertEquals(songList.getSelectedItemPosition(), prevPosSong);
        }
    }

    private MusicService getMusicSrv() throws Exception {
        Field field = main.getClass().getDeclaredField("musicSrv");
        field.setAccessible(true);
        return (MusicService) field.get(main);
    }

    /*
    public void testExit() throws Exception {
        solo.goBack();
    }
*/

    @Override
    public void tearDown() throws Exception {
        solo.finishOpenedActivities();
    }
}