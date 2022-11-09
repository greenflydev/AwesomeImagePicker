package in.myinnos.awesomeimagepicker.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.provider.MediaStore;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import in.myinnos.awesomeimagepicker.models.Media;
import in.myinnos.awesomeimagepicker.models.MediaStoreType;

/**
 * Created by MyInnos on 03-11-2016.
 */
public class ConstantsCustomGallery {
    public static final int PERMISSION_REQUEST_CODE = 1000;
    public static final int PERMISSION_GRANTED = 1001;
    public static final int PERMISSION_DENIED = 1002;

    public static final int REQUEST_CODE = 2000;

    public static final int FETCH_STARTED = 2001;
    public static final int FETCH_COMPLETED = 2002;
    public static final int FETCH_UPDATED = 2003;
    public static final int ERROR = 2005;
    public static final int EMPTY_LIST = 2006;

    /*
     * Fake albums for showing all photos or all videos.
     */
    public static final long ALL_PHOTOS_ALBUM_ID = -1001;
    public static final long ALL_VIDEOS_ALBUM_ID = -1002;

    /*
     * When looking at mixed media, at the top will be tabs to select just
     * photos or videos. These are the tab positions.
     */
    public static final int TAB_ALL_MEDIA_POSITION = 0;
    public static final int TAB_PHOTOS_POSITION = 1;
    public static final int TAB_VIDEOS_POSITION = 2;

    /*
     * The parent application may need to know when certain events happen. There will be a local
     * broadcast with action BROADCAST_EVENT and BROADCAST_EVENT_TYPE. This will let the parent
     * application know what event has triggered.
     *
     * Event type: BROADCAST_EVENT_ALBUM_SELECTED = true
     *  - Property: INTENT_EXTRA_ALBUM_ID = The album id selected
     *
     * Event type: BROADCAST_EVENT_FILTER_BY_TYPE = true
     *  - Property: INTENT_EXTRA_FILTER_BY_TYPE = the tab position for all media, photos, or videos
     */
    public static final String BROADCAST_EVENT = "BROADCAST_EVENT";
    public static final String BROADCAST_EVENT_ALBUM_SELECTED = "BROADCAST_EVENT_ALBUM_SELECTED";
    public static final String BROADCAST_EVENT_FILTER_BY_TYPE = "BROADCAST_EVENT_FILTER_BY_TYPE";

    public static final String INTENT_EXTRA_ALBUM_ID = "albumId";
    public static final String INTENT_EXTRA_ALBUM = "album";
    public static final String INTENT_EXTRA_MEDIA = "media";
    public static final String INTENT_EXTRA_LIMIT = "limit";
    public static final String INTENT_EXTRA_MEDIASTORETYPE = "mediaStoreType";
    public static final String INTENT_EXTRA_FILTER_BY_TYPE = "filterByType";
    public static final int DEFAULT_LIMIT = 10;

    public static final String SP_NAME_MAIN = "SP_NAME_MAIN";
    public static final String SP_PREVIOUSLY_SELECTED_IDS = "SP_PREVIOUSLY_SELECTED_IDS";

    /*
     * Holds a list of ids that the user has already selected to upload. So if the user comes back
     * while sending several batches they will know where they left off.
     */
    private static Set<String> previouslySelectedIds = null;

    public static final Map<String, Media> currentlySelectedMap = new HashMap<>();

    /*
     * Maximum number of media items that can be selected at a time
     */
    public static int limit = DEFAULT_LIMIT;

    //Type of media
    public static MediaStoreType mediaStoreType;

    public static Uri getQueryUri() {
        return MediaStore.Files.getContentUri("external"); // API 29 : MediaStore.VOLUME_EXTERNAL
    }

    /*
     * This will load the saved previously selected ids from shared preferences.
     * If they are already loaded then it will return it.
     */
    public static Set<String> getPreviouslySelectedIds(Context context) {
        if (previouslySelectedIds != null) {
            return previouslySelectedIds;
        }

        previouslySelectedIds = getStringSetFromMainSP(context, SP_PREVIOUSLY_SELECTED_IDS);
        if (previouslySelectedIds == null) {
            previouslySelectedIds = new HashSet<>();
        }
        return previouslySelectedIds;
    }

    /*
     * This will save the set of previously selected ids
     */
    public static void savePreviouslySelectedIds(Context context) {
        saveStringSetToMainSP(context, SP_PREVIOUSLY_SELECTED_IDS, previouslySelectedIds);
    }

    private static void saveStringSetToMainSP(Context context, String key, Set<String> value) {
        if (context != null) {
            SharedPreferences sp = context.getSharedPreferences(SP_NAME_MAIN, 0);
            sp.edit().putStringSet(key, value).commit();
        }
    }

    private static Set<String> getStringSetFromMainSP(Context context, String key) {
        if (context != null) {
            SharedPreferences sp = context.getSharedPreferences(SP_NAME_MAIN, 0);
            return sp.getStringSet(key, new HashSet<>());
        }
        return new HashSet<>();
    }

    public static int dpToPx(double dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }
}
