/*
 * Copyright 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.sampletvinput.rich;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Xml;

import com.example.android.sampletvinput.BaseTvInputService;
import com.example.android.sampletvinput.R;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * TvInputService which provides a full implementation of EPG, subtitles, multi-audio,
 * parental controls, and overlay view.
 */
public class RichTvInputService extends BaseTvInputService {
    private static final String TAG = "RichTvInputService";

    private static Uri mCatalogUri;

    private static List<ChannelInfo> sSampleChannels;
    private static TvInput sTvInput;

    private static final boolean USE_LOCAL_XML_FEED = true;

    @Override
    public List<ChannelInfo> createSampleChannels() {
        return createRichChannelsStatic(this);
    }

    public static List<ChannelInfo> createRichChannelsStatic(Context context) {
        mCatalogUri =
                USE_LOCAL_XML_FEED ?
                        Uri.parse("android.resource://" + context.getPackageName() + "/"
                                + R.raw.rich_tv_inputs_tif)
                        : Uri.parse(context.getResources().getString(R.string.catalog_url));
        synchronized (RichTvInputService.class) {
            if (sSampleChannels != null) {
                return sSampleChannels;
            }
            LoadTvInputTask inputTask = new LoadTvInputTask(context);
            inputTask.execute(mCatalogUri);

            try {
                inputTask.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            return sSampleChannels;
        }
    }

    public static TvInput getTvInput() {
        return sTvInput;
    }

    /**
     * AsyncTask for loading online channels.
     */
    private static class LoadTvInputTask extends AsyncTask<Uri, Void, Void> {

        private Context mContext;

        public LoadTvInputTask(Context context) {
            mContext = context;
        }

        @Override
        protected Void doInBackground(Uri... uris) {
            try {
                fetchUri(uris[0]);
            } catch (IOException e) {
            }
            return null;
        }

        private void fetchUri(Uri videoUri) throws IOException {
            InputStream inputStream = null;
            try {
                inputStream = mContext.getContentResolver().openInputStream(videoUri);
                XmlPullParser parser = Xml.newPullParser();
                try {
                    parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
                    parser.setInput(inputStream, null);
                    sTvInput = ChannelXMLParser.parseTvInput(parser);
                    sSampleChannels = ChannelXMLParser.parseChannelXML(parser);
                } catch (XmlPullParserException e) {
                    e.printStackTrace();
                }
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }
            }
        }

    }
}
