// Copyright 2026 Courville Software
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.archos.mediacenter.video.browser.loader;

import android.content.Context;

import com.archos.mediacenter.utils.trakt.Trakt;
import com.archos.mediaprovider.video.LoaderUtils;
import com.archos.mediaprovider.video.VideoStore;

/**
 * Loads media for "Continue Watching":
 * 1. In-progress movies (with active resume position).
 * 2. In-progress TV episodes (the current in-progress episode per show).
 * 3. The next unplayed episode for TV shows where previous episodes have been
 *    watched, provided no episode of that show is currently in progress.
 */
public class ContinueWatchingLoader extends VideoLoader {

    public ContinueWatchingLoader(Context context) {
        super(context);
        init();
        if (VideoLoader.ALLVIDEO_THROTTLE) setUpdateThrottle(VideoLoader.ALLVIDEO_THROTTLE_DELAY);
    }

    @Override
    public String getSelection() {
        StringBuilder sb = new StringBuilder();

        if (LoaderUtils.mustHideUserHiddenObjects()) {
            sb.append(LoaderUtils.HIDE_USER_HIDDEN_FILTER);
        }

        if (sb.length() > 0) sb.append(" AND ");

        sb.append("(");

        // 1. In-progress movies (s_id IS NULL)
        sb.append("(")
          .append(VideoStore.Video.VideoColumns.SCRAPER_SHOW_ID).append(" IS NULL AND ")
          .append(VideoStore.Video.VideoColumns.BOOKMARK).append(" > 0 AND ")
          .append(VideoStore.Video.VideoColumns.BOOKMARK).append(" != -2 AND (")
          .append(VideoStore.Video.VideoColumns.ARCHOS_TRAKT_SEEN).append(" IS NULL OR ")
          .append(VideoStore.Video.VideoColumns.ARCHOS_TRAKT_SEEN).append(" != ").append(Trakt.TRAKT_DB_MARKED).append(")")
          .append(")");

        sb.append(" OR ");

        // 2. In-progress TV episodes (at most one latest played episode per show)
        sb.append("(")
          .append(VideoStore.Video.VideoColumns.SCRAPER_SHOW_ID).append(" IS NOT NULL AND ")
          .append(VideoStore.Video.VideoColumns._ID).append(" IN (")
          .append("SELECT v1._id FROM video v1 JOIN (")
          .append("SELECT s_id, MAX(COALESCE(Archos_lastTimePlayed, 0) * 10000000 + _id) AS max_key ")
          .append("FROM video WHERE bookmark > 0 AND bookmark != -2 AND (Archos_traktSeen IS NULL OR Archos_traktSeen != ")
          .append(Trakt.TRAKT_DB_MARKED).append(") AND s_id IS NOT NULL ")
          .append("GROUP BY s_id")
          .append(") v2 ON v1.s_id = v2.s_id AND (COALESCE(v1.Archos_lastTimePlayed, 0) * 10000000 + v1._id) = v2.max_key")
          .append(")")
          .append(")");

        sb.append(" OR ");

        // 3. Next unplayed episode for TV shows where previous episodes were watched
        //    and NO episode of that show is currently in progress
        sb.append("(")
          .append(VideoStore.Video.VideoColumns.SCRAPER_SHOW_ID).append(" IS NOT NULL AND ")
          .append(VideoStore.Video.VideoColumns._ID).append(" IN (")
          .append("SELECT _id FROM (")
          .append("SELECT v._id, MIN(COALESCE(v.e_season, 0) * 10000 + COALESCE(v.e_episode, 0)) ")
          .append("FROM video v JOIN (")
          .append("SELECT s_id, COALESCE(MIN(CASE WHEN e_season > 0 THEN e_season END), 0) AS min_watched_season ")
          .append("FROM video WHERE (bookmark = -2 OR Archos_traktSeen = ").append(Trakt.TRAKT_DB_MARKED).append(") ")
          .append("AND s_id IS NOT NULL ")
          .append("AND s_id NOT IN (")
          .append("SELECT s_id FROM video WHERE bookmark > 0 AND bookmark != -2 AND (Archos_traktSeen IS NULL OR Archos_traktSeen != ")
          .append(Trakt.TRAKT_DB_MARKED).append(") AND s_id IS NOT NULL")
          .append(") ")
          .append("GROUP BY s_id")
          .append(") ws ON v.s_id = ws.s_id ")
          .append("WHERE (v.bookmark IS NULL OR v.bookmark <= 0) ")
          .append("AND (v.bookmark != -2) ")
          .append("AND (v.Archos_traktSeen IS NULL OR v.Archos_traktSeen != ").append(Trakt.TRAKT_DB_MARKED).append(") ")
          .append("AND COALESCE(v.e_season, 0) >= ws.min_watched_season ")
          .append("GROUP BY v.s_id")
          .append(")")
          .append(")")
          .append(")");

        sb.append(")");

        return sb.toString();
    }

    @Override
    public String getSortOrder() {
        return "COALESCE(" +
                "CASE WHEN " + VideoStore.Video.VideoColumns.ARCHOS_LAST_TIME_PLAYED + " > 0 THEN " +
                    VideoStore.Video.VideoColumns.ARCHOS_LAST_TIME_PLAYED + " " +
                "ELSE (" +
                    "SELECT MAX(lw." + VideoStore.Video.VideoColumns.ARCHOS_LAST_TIME_PLAYED + ") " +
                    "FROM video lw " +
                    "WHERE lw." + VideoStore.Video.VideoColumns.SCRAPER_SHOW_ID + " = video." + VideoStore.Video.VideoColumns.SCRAPER_SHOW_ID + " " +
                    "AND (lw." + VideoStore.Video.VideoColumns.BOOKMARK + " = -2 OR lw." + VideoStore.Video.VideoColumns.ARCHOS_TRAKT_SEEN + " = " + Trakt.TRAKT_DB_MARKED + ")" +
                ") " +
                "END, 0) DESC, " + VideoStore.Video.VideoColumns._ID + " DESC LIMIT 50";
    }
}
