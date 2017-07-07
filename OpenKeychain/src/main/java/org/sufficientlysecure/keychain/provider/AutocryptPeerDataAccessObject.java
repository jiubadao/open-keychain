/*
 * Copyright (C) 2012-2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2014-2016 Vincent Breitmoser <v.breitmoser@mugenguild.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.sufficientlysecure.keychain.provider;


import java.util.Date;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import org.sufficientlysecure.keychain.provider.KeychainContract.ApiAutocryptPeer;


public class AutocryptPeerDataAccessObject {
    private final SimpleContentResolverInterface mQueryInterface;
    private final String packageName;


    public AutocryptPeerDataAccessObject(Context context, String packageName) {
        this.packageName = packageName;

        final ContentResolver contentResolver = context.getContentResolver();
        mQueryInterface = new SimpleContentResolverInterface() {
            @Override
            public Cursor query(Uri contentUri, String[] projection, String selection, String[] selectionArgs,
                    String sortOrder) {
                return contentResolver.query(contentUri, projection, selection, selectionArgs, sortOrder);
            }

            @Override
            public Uri insert(Uri contentUri, ContentValues values) {
                return contentResolver.insert(contentUri, values);
            }

            @Override
            public int update(Uri contentUri, ContentValues values, String where, String[] selectionArgs) {
                return contentResolver.update(contentUri, values, where, selectionArgs);
            }

            @Override
            public int delete(Uri contentUri, String where, String[] selectionArgs) {
                return contentResolver.delete(contentUri, where, selectionArgs);
            }
        };
    }

    public AutocryptPeerDataAccessObject(SimpleContentResolverInterface queryInterface, String packageName) {
        mQueryInterface = queryInterface;
        this.packageName = packageName;
    }

    public Long getMasterKeyIdForAutocryptPeer(String autocryptId) {
        Cursor cursor = mQueryInterface.query(
                ApiAutocryptPeer.buildByPackageNameAndAutocryptId(packageName, autocryptId), null, null, null, null);

        try {
            if (cursor != null && cursor.moveToFirst()) {
                int masterKeyIdColumn = cursor.getColumnIndex(ApiAutocryptPeer.MASTER_KEY_ID);
                return cursor.getLong(masterKeyIdColumn);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return null;
    }

    public Date getLastUpdateForAutocryptPeer(String autocryptId) {
        Cursor cursor = mQueryInterface.query(ApiAutocryptPeer.buildByPackageNameAndAutocryptId(packageName, autocryptId),
                null, null, null, null);

        try {
            if (cursor != null && cursor.moveToFirst()) {
                long lastUpdated = cursor.getColumnIndex(ApiAutocryptPeer.LAST_UPDATED);
                return new Date(lastUpdated);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    public void setMasterKeyIdForAutocryptPeer(String autocryptId, long masterKeyId, Date date) {
        Date lastUpdated = getLastUpdateForAutocryptPeer(autocryptId);
        if (lastUpdated != null && lastUpdated.after(date)) {
            throw new IllegalArgumentException("Database entry was newer than the one to be inserted! Cannot backdate");
        }

        ContentValues cv = new ContentValues();
        cv.put(ApiAutocryptPeer.MASTER_KEY_ID, masterKeyId);
        cv.put(ApiAutocryptPeer.LAST_UPDATED, date.getTime());
        mQueryInterface.update(ApiAutocryptPeer.buildByPackageNameAndAutocryptId(packageName, autocryptId), cv, null, null);
    }
}
