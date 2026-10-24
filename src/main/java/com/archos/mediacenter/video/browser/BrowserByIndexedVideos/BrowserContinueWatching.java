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

package com.archos.mediacenter.video.browser.BrowserByIndexedVideos;

import android.database.Cursor;
import android.os.Bundle;
import androidx.loader.content.Loader;

import com.archos.mediacenter.video.R;
import com.archos.mediacenter.video.browser.loader.ContinueWatchingLoader;
import com.archos.mediacenter.video.utils.VideoUtils;

public class BrowserContinueWatching extends CursorBrowserByVideo {

    @Override
    public int getDefaultViewMode() {
        return VideoUtils.VIEW_MODE_GRID;
    }

    @Override
    public int getEmptyMessage() {
        return R.string.you_have_no_continue_watching_video;
    }

    @Override
    protected String getActionBarTitle() {
        return getString(R.string.continue_watching);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new ContinueWatchingLoader(getContext()).getV4CursorLoader(true, false);
    }
}
